package com.example.audit.infra.maintenance;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.example.audit.config.AuditRetentionProperties;
import com.example.audit.infra.persistence.AuditLogRepository;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;
import com.example.common.schedule.BatchJobCode;
import com.example.common.schedule.ScheduledJobPort;
import com.example.common.schedule.TriggerDescriptor;
import com.example.common.schedule.TriggerType;

/**
 * 보존 기간을 초과한 감사 로그를 주기적으로 정리하는 잡.
 * 정책 연동 시 retentionDays 값을 외부 설정으로부터 주입받도록 확장할 수 있다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditLogRetentionJob implements ScheduledJobPort, org.springframework.scheduling.annotation.SchedulingConfigurer {

    private static final String DEFAULT_CRON = "0 0 3 * * *";
    private static final boolean DEFAULT_ENABLED = true;

    private final AuditLogRepository repository;
    private final Clock clock;
    private final PolicySettingsProvider policySettingsProvider;
    private final AuditRetentionProperties retentionProperties;
    /**
     * 기본 3년(1095일) 보존. 정책/설정으로 주입 가능.
     */
    private long retentionDays = 1095;
    @org.springframework.beans.factory.annotation.Value("${central.scheduler.enabled:false}")
    private boolean centralSchedulerEnabled;

    public void purgeExpired() {
        PolicyToggleSettings settings = policySettingsProvider.currentSettings();
        boolean enabled = settings != null ? settings.auditLogRetentionEnabled() : DEFAULT_ENABLED;
        if (!enabled) {
            return;
        }
        long days = settings != null ? settings.auditRetentionDays() : retentionProperties.days();
        if (days <= 0) {
            days = retentionDays;
        }
        Instant threshold = clock.instant().minus(Duration.ofDays(days));
        long deleted = repository.deleteByEventTimeBefore(threshold);
        if (deleted > 0) {
            log.info("Audit retention purge deleted {} rows older than {}", deleted, threshold);
        }
    }

    public void setRetentionDays(long retentionDays) {
        this.retentionDays = retentionDays;
    }

    // ScheduledJobPort
    @Override public String jobId() { return "audit-log-retention"; }
    @Override public void runOnce(java.time.Instant now) { purgeExpired(); }
    @Override public TriggerDescriptor trigger() {
        PolicyToggleSettings settings = policySettingsProvider.currentSettings();
        var policy = policySettingsProvider.batchJobSchedule(BatchJobCode.AUDIT_LOG_RETENTION);
        if (policy != null) {
            return policy.toTriggerDescriptor();
        }
        boolean enabled = settings != null ? settings.auditLogRetentionEnabled() : DEFAULT_ENABLED;
        String cron = settings != null && settings.auditLogRetentionCron() != null && !settings.auditLogRetentionCron().isBlank()
                ? settings.auditLogRetentionCron()
                : DEFAULT_CRON;
        return new TriggerDescriptor(enabled, TriggerType.CRON, cron, 0, 0, null);
    }

    @org.springframework.context.event.EventListener
    public void onPolicyChanged(com.example.common.policy.PolicyChangedEvent event) {
        if ("security.policy".equals(event.code())) {
            log.info("[audit-retention] policy changed, next trigger will reflect updated settings");
        }
    }

    // 기존 @Scheduled fallback (central off)
    @Override
    public void configureTasks(org.springframework.scheduling.config.ScheduledTaskRegistrar taskRegistrar) {
        if (centralSchedulerEnabled) {
            log.info("[audit-retention] central scheduler enabled, skipping local registration");
            return;
        }
        taskRegistrar.addTriggerTask(this::purgeExpired, triggerContext -> new org.springframework.scheduling.support.CronTrigger(trigger().expression()).nextExecution(triggerContext));
    }
}
