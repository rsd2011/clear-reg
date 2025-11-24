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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.beans.factory.ObjectProvider;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.schedule.BatchJobCode;
import com.example.common.schedule.BatchJobDefaults;
import com.example.common.schedule.BatchJobSchedule;
import com.example.common.schedule.ScheduledJobPort;
import com.example.common.schedule.TriggerDescriptor;

/**
 * file_audit_outbox 테이블에서 대기중(PENDING) 이벤트를 읽어 전송 후 SENT로 마킹하는 릴레이.
 */
@Component
public class FileAuditOutboxRelay implements ScheduledJobPort {

    private static final Logger log = LoggerFactory.getLogger(FileAuditOutboxRelay.class);

    private final JdbcTemplate jdbcTemplate;
    private final FileAuditPublisher targetPublisher;
    private final int batchSize;
    private final long relayIntervalMs;
    private final PolicySettingsProvider policySettingsProvider;
    private final boolean centralSchedulerEnabled;

    public FileAuditOutboxRelay(JdbcTemplate jdbcTemplate,
                                FileAuditPublisher targetPublisher,
                                @Value("${file.audit.outbox.batch-size:50}") int batchSize,
                                @Value("${file.audit.outbox.relay-interval-ms:5000}") long relayIntervalMs,
                                ObjectProvider<PolicySettingsProvider> policySettingsProvider,
                                @Value("${central.scheduler.enabled:false}") boolean centralSchedulerEnabled) {
        this.jdbcTemplate = jdbcTemplate;
        this.targetPublisher = targetPublisher;
        this.batchSize = batchSize;
        this.relayIntervalMs = relayIntervalMs;
        this.policySettingsProvider = policySettingsProvider.getIfAvailable();
        this.centralSchedulerEnabled = centralSchedulerEnabled;
    }

    /** 테스트/기존 사용처 호환용 단축 생성자. */
    // 단축 생성자 제거(테스트는 ObjectProvider mock 사용)

    @Scheduled(fixedDelayString = "${file.audit.outbox.relay-interval-ms:5000}")
    @Transactional
    public void relay() {
        if (centralSchedulerEnabled) {
            return;
        }
        runOnce(java.time.Instant.now());
    }

    @Override
    public String jobId() {
        return BatchJobCode.FILE_AUDIT_OUTBOX_RELAY.name();
    }

    @Override
    public TriggerDescriptor trigger() {
        return resolveSchedule().toTriggerDescriptor();
    }

    @Override
    public void runOnce(java.time.Instant now) {
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

    private BatchJobSchedule resolveSchedule() {
        BatchJobSchedule policy = policySettingsProvider == null ? null
                : policySettingsProvider.batchJobSchedule(BatchJobCode.FILE_AUDIT_OUTBOX_RELAY);
        if (policy != null) {
            return policy;
        }
        long interval = relayIntervalMs > 0 ? relayIntervalMs : BatchJobDefaults.defaults().get(BatchJobCode.FILE_AUDIT_OUTBOX_RELAY).fixedDelayMillis();
        return new BatchJobSchedule(true, com.example.common.schedule.TriggerType.FIXED_DELAY, null, interval, 0, null);
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
