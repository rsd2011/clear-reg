package com.example.audit.infra;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import com.example.audit.config.AuditRetentionProperties;
import com.example.audit.infra.persistence.AuditLogRepository;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;

/**
 * 보존기간이 지난 감사 로그를 주기적으로 정리하는 잡.
 * 파티션 사용 시 파티션 DROP 배치로 대체 가능.
 */
@Component
public class RetentionCleanupJob implements SchedulingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(RetentionCleanupJob.class);
    private static final String DEFAULT_CRON = "0 30 3 * * *";
    private static final boolean DEFAULT_ENABLED = true;

    private final AuditLogRepository repository;
    private final AuditRetentionProperties properties;
    private final PolicySettingsProvider policySettingsProvider;

    public RetentionCleanupJob(AuditLogRepository repository, AuditRetentionProperties properties, PolicySettingsProvider policySettingsProvider) {
        this.repository = repository;
        this.properties = properties;
        this.policySettingsProvider = policySettingsProvider;
    }

    public void purgeExpired() {
        PolicyToggleSettings settings = policySettingsProvider.currentSettings();
        boolean enabled = settings != null ? settings.auditRetentionCleanupEnabled() : DEFAULT_ENABLED;
        if (!enabled) {
            return;
        }
        int days = properties.days();
        Instant threshold = Instant.now().minus(days, ChronoUnit.DAYS);
        long deleted = repository.deleteByEventTimeBefore(threshold);
        if (deleted > 0) {
            log.info("audit retention purge deleted={} before={}days", deleted, days);
        }
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(this::purgeExpired, triggerContext -> {
            PolicyToggleSettings settings = policySettingsProvider.currentSettings();
            String cron = settings != null && settings.auditRetentionCleanupCron() != null && !settings.auditRetentionCleanupCron().isBlank()
                    ? settings.auditRetentionCleanupCron()
                    : DEFAULT_CRON;
            return new CronTrigger(cron).nextExecution(triggerContext);
        });
    }
}
