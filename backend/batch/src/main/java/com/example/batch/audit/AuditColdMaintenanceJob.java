package com.example.batch.audit;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;
import com.example.common.schedule.ScheduledJobPort;
import com.example.common.schedule.TriggerDescriptor;
import com.example.common.schedule.TriggerType;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

/**
 * HOT 보존 기간이 지난 파티션을 COLD 테이블스페이스로 이동하고 재색인하는 경량 잡.
 * 파티션 이름 규칙: audit_log_yyyy_MM
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditColdMaintenanceJob implements ScheduledJobPort, SchedulingConfigurer {

    private final DataSource dataSource;
    private final Clock clock;
    private final PolicySettingsProvider policySettingsProvider;

    @Value("${audit.partition.tablespace.cold:}")
    private String coldTablespace;

    @Value("${audit.partition.hot-months:6}")
    private int hotMonths;

    @Value("${audit.cold-maintenance.cron:0 0 5 * * 0}")
    private String cron;

    @Value("${central.scheduler.enabled:false}")
    private boolean centralSchedulerEnabled;

    /**
     * 주 1회 실행: HOT 기간을 지난 지난달 파티션을 cold TS로 이동.
     */
    public void moveOldPartitions() {
        if (!isEnabled()) {
            return;
        }
        LocalDate targetMonth = LocalDate.now(clock).minusMonths(hotMonths + 1).withDayOfMonth(1);
        String suffix = targetMonth.format(DateTimeFormatter.ofPattern("yyyy_MM"));
        String partition = "audit_log_" + suffix;
        String alter = "ALTER TABLE %s SET TABLESPACE %s".formatted(partition, coldTablespace);
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute(alter);
            log.info("[audit-cold] moved {} to tablespace {}", partition, coldTablespace);
            try {
                conn.createStatement().execute("REINDEX TABLE " + partition);
                log.info("[audit-cold] reindex {}", partition);
            } catch (SQLException e) {
                log.warn("[audit-cold] reindex failed {}: {}", partition, e.getMessage());
            }
        } catch (SQLException e) {
            log.warn("[audit-cold] move failed {}: {}", partition, e.getMessage());
        }
    }

    public boolean isEnabled() {
        PolicyToggleSettings settings = policySettingsProvider.currentSettings();
        boolean enabled = settings != null ? settings.auditColdArchiveEnabled() : true;
        if (!enabled) {
            return false;
        }
        return coldTablespace != null && !coldTablespace.isBlank();
    }

    public String currentCron() {
        PolicyToggleSettings settings = policySettingsProvider.currentSettings();
        String policyCron = settings != null ? settings.auditColdArchiveCron() : null;
        if (policyCron == null || policyCron.isBlank()) {
            return cron == null || cron.isBlank() ? "0 0 5 * * 0" : cron;
        }
        return policyCron;
    }

    @Override
    public String jobId() {
        return "audit-cold-maintenance";
    }

    @Override
    public TriggerDescriptor trigger() {
        var policy = policySettingsProvider.batchJobSchedule(com.example.common.schedule.BatchJobCode.AUDIT_COLD_MAINTENANCE);
        if (policy != null) {
            return policy.toTriggerDescriptor();
        }
        return new TriggerDescriptor(isEnabled(), TriggerType.CRON, currentCron(), 0, 0, null);
    }

    @Override
    public void runOnce(java.time.Instant now) {
        moveOldPartitions();
    }

    @Override
    public void configureTasks(org.springframework.scheduling.config.ScheduledTaskRegistrar taskRegistrar) {
        if (centralSchedulerEnabled) {
            log.info("[audit-cold] central scheduler enabled, skipping local registration");
            return;
        }
        taskRegistrar.addTriggerTask(this::moveOldPartitions, triggerContext ->
                new org.springframework.scheduling.support.CronTrigger(currentCron()).nextExecution(triggerContext));
    }

    @EventListener
    public void onPolicyChanged(com.example.common.policy.PolicyChangedEvent event) {
        if ("security.policy".equals(event.code())) {
            log.info("[audit-cold-maintenance] policy changed, next trigger will use updated settings");
        }
    }
}
