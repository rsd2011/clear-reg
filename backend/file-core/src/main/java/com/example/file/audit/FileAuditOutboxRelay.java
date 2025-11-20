package com.example.file.audit;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * file_audit_outbox 테이블에서 대기중(PENDING) 이벤트를 읽어 전송 후 SENT로 마킹하는 릴레이.
 */
@Component
public class FileAuditOutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(FileAuditOutboxRelay.class);

    private final JdbcTemplate jdbcTemplate;
    private final FileAuditPublisher targetPublisher;
    private final int batchSize;

    public FileAuditOutboxRelay(JdbcTemplate jdbcTemplate,
                                FileAuditPublisher targetPublisher,
                                @Value("${file.audit.outbox.batch-size:50}") int batchSize) {
        this.jdbcTemplate = jdbcTemplate;
        this.targetPublisher = targetPublisher;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${file.audit.outbox.relay-interval-ms:5000}")
    @Transactional
    public void relay() {
        List<FileAuditOutboxRow> rows = jdbcTemplate.query("""
                select id, action, file_id, actor, occurred_at, payload
                  from file_audit_outbox
                 where status = 'PENDING'
                 order by available_at asc
                 limit ?
                """, new FileAuditOutboxRowMapper(), batchSize);
        if (rows.isEmpty()) {
            return;
        }
        for (FileAuditOutboxRow row : rows) {
            try {
                targetPublisher.publish(new FileAuditEvent(row.action(), row.fileId(), row.actor(), row.occurredAt()));
                jdbcTemplate.update("update file_audit_outbox set status = 'SENT', last_error = null where id = ?", row.id());
            } catch (Exception ex) {
                log.warn("file-audit outbox relay failed id={} action={} error={}", row.id(), row.action(), ex.getMessage());
                jdbcTemplate.update("update file_audit_outbox set status = 'FAILED', last_error = ? where id = ?", ex.getMessage(), row.id());
            }
        }
    }

    private record FileAuditOutboxRow(UUID id, String action, UUID fileId, String actor, OffsetDateTime occurredAt, String payload) {
    }

    private static class FileAuditOutboxRowMapper implements RowMapper<FileAuditOutboxRow> {
        @Override
        public FileAuditOutboxRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            UUID id = rs.getObject("id", UUID.class);
            String action = rs.getString("action");
            UUID fileId = rs.getObject("file_id", UUID.class);
            String actor = rs.getString("actor");
            Timestamp ts = rs.getTimestamp("occurred_at");
            OffsetDateTime occurredAt = ts.toInstant().atOffset(ZoneOffset.UTC);
            String payload = rs.getString("payload");
            return new FileAuditOutboxRow(id, action, fileId, actor, occurredAt, payload);
        }
    }
}
