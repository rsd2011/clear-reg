package com.example.draft.application.audit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "draft.audit.publisher", havingValue = "outbox")
public class OutboxDraftAuditPublisher implements DraftAuditPublisher {

    private final JdbcTemplate jdbcTemplate;

    public OutboxDraftAuditPublisher(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void publish(DraftAuditEvent event) {
        // 단순 outbox 테이블 적재. 컨슈머가 SIEM/Kafka 등으로 전달하도록 설계.
        jdbcTemplate.update("""
                INSERT INTO audit_outbox (id, aggregate_id, aggregate_type, payload, occurred_at, status)
                VALUES (?, ?, ?, to_json(?::json), ?, 'PENDING')
                """,
                UUID.randomUUID(),
                event.draftId(),
                "DRAFT",
                AuditJsonSerializer.serialize(event),
                event.occurredAt());
    }
}
