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

/**
 * Outbox에서 감사 이벤트를 가져와 전달(로깅/추후 커넥터)하는 간단한 릴레이.
 * draft.audit.outbox.relay.enabled=true 일 때만 동작.
 */
@Component
@ConditionalOnProperty(name = "draft.audit.outbox.relay.enabled", havingValue = "true")
public class OutboxDraftAuditRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxDraftAuditRelay.class);
    private final JdbcTemplate jdbcTemplate;

    public OutboxDraftAuditRelay(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedDelayString = "${draft.audit.outbox.relay.delay-ms:60000}")
    public void relay() {
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
}
