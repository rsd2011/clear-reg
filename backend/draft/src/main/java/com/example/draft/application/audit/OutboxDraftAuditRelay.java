package com.example.draft.application.audit;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;

import com.example.common.policy.PolicySettingsProvider;
import com.example.common.schedule.BatchJobCode;
import com.example.common.schedule.BatchJobDefaults;
import com.example.common.schedule.BatchJobSchedule;
import com.example.common.schedule.ScheduledJobPort;
import com.example.common.schedule.TriggerDescriptor;

/**
 * Outbox에서 감사 이벤트를 가져와 전달(로깅/추후 커넥터)하는 간단한 릴레이.
 * draft.audit.outbox.relay.enabled=true 일 때만 동작.
 */
@Component
@ConditionalOnProperty(name = "draft.audit.outbox.relay.enabled", havingValue = "true")
public class OutboxDraftAuditRelay implements ScheduledJobPort {

    private static final Logger log = LoggerFactory.getLogger(OutboxDraftAuditRelay.class);
    private final JdbcTemplate jdbcTemplate;
    private final long delayMs;
    private final PolicySettingsProvider policySettingsProvider;
    private final boolean centralSchedulerEnabled;

    public OutboxDraftAuditRelay(JdbcTemplate jdbcTemplate,
                                 @Value("${draft.audit.outbox.relay.delay-ms:60000}") long delayMs,
                                 ObjectProvider<PolicySettingsProvider> policySettingsProvider,
                                 @Value("${central.scheduler.enabled:false}") boolean centralSchedulerEnabled) {
        this.jdbcTemplate = jdbcTemplate;
        this.delayMs = delayMs;
        this.policySettingsProvider = policySettingsProvider.getIfAvailable();
        this.centralSchedulerEnabled = centralSchedulerEnabled;
    }

    // 단축 생성자 제거(테스트는 ObjectProvider mock 사용)

    @Scheduled(fixedDelayString = "${draft.audit.outbox.relay.delay-ms:60000}")
    public void relay() {
        if (centralSchedulerEnabled) {
            return;
        }
        runOnce(java.time.Instant.now());
    }

    @Override
    public String jobId() {
        return BatchJobCode.DRAFT_AUDIT_OUTBOX_RELAY.name();
    }

    @Override
    public TriggerDescriptor trigger() {
        return resolveSchedule().toTriggerDescriptor();
    }

    @Override
    public void runOnce(java.time.Instant now) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, payload
                FROM audit_outbox
                WHERE status = 'PENDING' AND (next_retry_at IS NULL OR next_retry_at <= now())
                ORDER BY occurred_at
                LIMIT 50
                """);

        for (Map<String, Object> row : rows) {
            UUID id = (UUID) row.get("id");
            String payload = row.get("payload").toString();
            // TODO: 실제 SIEM/Kafka 커넥터로 전달하도록 확장
            log.info("Relaying audit outbox id={} payload={}", id, payload);
            jdbcTemplate.update("UPDATE audit_outbox SET status='SENT', attempts=attempts+1 WHERE id=?", id);
        }
    }

    private BatchJobSchedule resolveSchedule() {
        BatchJobSchedule policy = policySettingsProvider == null ? null
                : policySettingsProvider.batchJobSchedule(BatchJobCode.DRAFT_AUDIT_OUTBOX_RELAY);
        if (policy != null) {
            return policy;
        }
        long interval = delayMs > 0 ? delayMs : BatchJobDefaults.defaults().get(BatchJobCode.DRAFT_AUDIT_OUTBOX_RELAY).fixedDelayMillis();
        return new BatchJobSchedule(true, com.example.common.schedule.TriggerType.FIXED_DELAY, null, interval, 0, null);
    }
}
