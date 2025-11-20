package com.example.file.audit;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "file.audit.publisher", havingValue = "outbox")
public class OutboxFileAuditPublisher implements FileAuditPublisher {

    private final JdbcTemplate jdbcTemplate;

    public OutboxFileAuditPublisher(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void publish(FileAuditEvent event) {
        String sql = """
                insert into file_audit_outbox (id, action, file_id, actor, occurred_at, payload, status, available_at, created_at)
                values (?, ?, ?, ?, ?, to_jsonb(?::json), 'PENDING', ?, ?)
                """;
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(sql,
                UUID.randomUUID(),
                event.action(),
                event.fileId(),
                event.actor(),
                Timestamp.from(event.occurredAt().toInstant()),
                AuditJsonSerializer.serialize(event),
                Timestamp.from(now.toInstant()),
                Timestamp.from(now.toInstant()));
    }
}
