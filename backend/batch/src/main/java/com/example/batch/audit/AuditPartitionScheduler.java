package com.example.batch.audit;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;

/**
 * PostgreSQL 기준 월 단위 파티션을 사전 생성하는 배치 스케줄러.
 * 정책/프로퍼티로 enable/cron/preloadMonths를 조정할 수 있다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditPartitionScheduler implements SchedulingConfigurer {

    private final DataSource dataSource;
    private final Clock clock;
    private final PolicySettingsProvider policySettingsProvider;

    @Value("${audit.partition.enabled:false}")
    private boolean enabledFallback;
    @Value("${audit.partition.cron:0 0 2 1 * *}")
    private String cronFallback;
    @Value("${audit.partition.preload-months:1}")
    private int preloadMonthsFallback;

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(this::createNextPartitions, triggerContext -> {
            String cron = currentSettings().auditPartitionCron();
            return new CronTrigger(cron).nextExecution(triggerContext);
        });
    }

    void createNextPartitions() {
        PolicyToggleSettings settings = currentSettings();
        if (!settings.auditPartitionEnabled()) {
            return;
        }
        int months = Math.max(settings.auditPartitionPreloadMonths(), 0);
        LocalDate start = LocalDate.now(clock).plusMonths(1).withDayOfMonth(1);
        IntStream.rangeClosed(0, months)
                .forEach(offset -> ensurePartition(start.plusMonths(offset)));
    }

    void ensurePartition(LocalDate monthStart) {
        String suffix = monthStart.format(DateTimeFormatter.ofPattern("yyyy_MM"));
        String partitionName = "audit_log_" + suffix;
        String ddl = """
                CREATE TABLE IF NOT EXISTS %s PARTITION OF audit_log
                FOR VALUES FROM ('%s') TO ('%s');
                """.formatted(partitionName, monthStart, monthStart.plusMonths(1));
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute(ddl);
            log.info("Audit partition ensured: {}", partitionName);
        } catch (SQLException e) {
            log.warn("Failed to create audit partition {}: {}", partitionName, e.getMessage());
        }
    }

    private PolicyToggleSettings currentSettings() {
        PolicyToggleSettings settings = policySettingsProvider.currentSettings();
        if (settings == null) {
            return new PolicyToggleSettings(true, true, true, null, 0L, null, true, 0,
                    true, true, true, 0, true, "MEDIUM", true, null, null,
                    enabledFallback, cronFallback, preloadMonthsFallback,
                    true, "0 0 4 1 * *");
        }
        boolean enabled = settings.auditPartitionEnabled();
        String cron = settings.auditPartitionCron() == null || settings.auditPartitionCron().isBlank()
                ? cronFallback : settings.auditPartitionCron();
        int preload = settings.auditPartitionPreloadMonths() < 0 ? preloadMonthsFallback : settings.auditPartitionPreloadMonths();
        return new PolicyToggleSettings(settings.passwordPolicyEnabled(), settings.passwordHistoryEnabled(), settings.accountLockEnabled(),
                settings.enabledLoginTypes(), settings.maxFileSizeBytes(), settings.allowedFileExtensions(), settings.strictMimeValidation(),
                settings.fileRetentionDays(), settings.auditEnabled(), settings.auditReasonRequired(), settings.auditSensitiveApiDefaultOn(),
                settings.auditRetentionDays(), settings.auditStrictMode(), settings.auditRiskLevel(), settings.auditMaskingEnabled(),
                settings.auditSensitiveEndpoints(), settings.auditUnmaskRoles(),
                enabled, cron, preload, settings.auditMonthlyReportEnabled(), settings.auditMonthlyReportCron());
    }
}
