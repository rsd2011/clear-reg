package com.example.dw.application.job;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.audit.Actor;
import com.example.audit.ActorType;
import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPort;
import com.example.audit.RiskLevel;
import com.example.audit.Subject;
import com.example.dw.domain.DwIngestionOutbox;
import com.example.dw.domain.DwIngestionOutboxRepository;

@Service
public class DwIngestionOutboxService {

    private static final Logger log = LoggerFactory.getLogger(DwIngestionOutboxService.class);
    private static final int MAX_RETRY = 5;

    private final DwIngestionOutboxRepository repository;
    private final Clock clock;
    private final AuditPort auditPort;

    public DwIngestionOutboxService(DwIngestionOutboxRepository repository, Clock clock, AuditPort auditPort) {
        this.repository = repository;
        this.clock = clock;
        this.auditPort = auditPort;
    }

    @Transactional
    public void enqueue(DwIngestionJob job) {
        DwIngestionOutbox entry = DwIngestionOutbox.pending(job.type(), clock);
        if (job.payload() != null) {
            entry.setPayload(job.payload());
        }
        try {
            repository.save(entry);
            audit("OUTBOX_ENQUEUE", entry.getId(), entry.getJobType().name(), entry.getStatus().name(), true);
        }
        catch (DataIntegrityViolationException ex) {
            log.warn("Duplicate outbox entry skipped: {}", job.type(), ex);
            audit("OUTBOX_ENQUEUE_DUP", null, job.type().name(), "DUPLICATE", false);
        }
    }

    @Transactional
    public List<DwIngestionOutbox> claimPending(int batchSize) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<DwIngestionOutbox> pending = repository
                .lockOldestPending(DwIngestionOutboxStatus.PENDING, now, org.springframework.data.domain.PageRequest.of(0, batchSize));
        pending.forEach(entry -> entry.markSending(clock, "dw-outbox-relay"));
        audit("OUTBOX_CLAIM", null, "batchSize=" + batchSize, "SENDING", true, "count", pending.size());
        return pending;
    }

    @Transactional
    public void markCompleted(UUID entryId) {
        updateEntry(entryId, entity -> {
            entity.markSent(clock);
            audit("OUTBOX_SENT", entryId, entity.getJobType().name(), entity.getStatus().name(), true);
        });
    }

    @Transactional
    public void markFailed(DwIngestionOutbox entry) {
        entry.markFailed(clock);
        repository.save(entry);
        audit("OUTBOX_FAILED", entry.getId(), entry.getJobType().name(), entry.getStatus().name(), false);
    }

    @Transactional
    public void markFailed(UUID entryId, String errorMessage) {
        updateEntry(entryId, entity -> {
            entity.markFailed(clock, errorMessage);
            audit("OUTBOX_FAILED", entryId, entity.getJobType().name(), entity.getStatus().name(), false, "error", errorMessage);
        });
    }

    @Transactional
    public void scheduleRetry(UUID entryId, Duration delay, String lastErrorMessage) {
        updateEntry(entryId, entity -> {
            if (entity.getRetryCount() >= MAX_RETRY) {
                entity.markDeadLetter(clock, lastErrorMessage);
                audit("OUTBOX_DEAD_LETTER", entryId, entity.getJobType().name(), entity.getStatus().name(), false, "error", lastErrorMessage);
            } else {
                entity.markRetry(clock, delay, lastErrorMessage);
                audit("OUTBOX_RETRY", entryId, entity.getJobType().name(), entity.getStatus().name(), false,
                        "retryCount", entity.getRetryCount());
            }
        });
    }

    @Transactional
    public void markDeadLetter(UUID entryId, String lastErrorMessage) {
        updateEntry(entryId, entity -> {
            entity.markDeadLetter(clock, lastErrorMessage);
            audit("OUTBOX_DEAD_LETTER", entryId, entity.getJobType().name(), entity.getStatus().name(), false, "error", lastErrorMessage);
        });
    }

    private void updateEntry(UUID entryId, Consumer<DwIngestionOutbox> consumer) {
        Optional<DwIngestionOutbox> entry = repository.findById(entryId);
        entry.ifPresent(entity -> {
            consumer.accept(entity);
            repository.save(entity);
        });
    }

    private void audit(String action, UUID entryId, String subjectKey, String resultCode, boolean success) {
        audit(action, entryId, subjectKey, resultCode, success, null, null);
    }

    private void audit(String action, UUID entryId, String subjectKey, String resultCode, boolean success,
                       String extraKey, Object extraValue) {
        try {
            AuditEvent.AuditEventBuilder builder = AuditEvent.builder()
                    .eventType("DW_OUTBOX")
                    .moduleName("dw-integration")
                    .action(action)
                    .subject(Subject.builder()
                            .type("DW_OUTBOX")
                            .key(subjectKey)
                            .build())
                    .actor(Actor.builder()
                            .id("dw-ingestion")
                            .type(ActorType.SYSTEM)
                            .build())
                    .resultCode(resultCode)
                    .success(success)
                    .riskLevel(success ? RiskLevel.LOW : RiskLevel.MEDIUM);
            if (extraKey != null) {
                builder.extraEntry(extraKey, extraValue);
            }
            if (entryId != null) {
                builder.extraEntry("entryId", entryId);
            }
            auditPort.record(builder.build(), AuditMode.ASYNC_FALLBACK);
        } catch (Exception e) {
            log.debug("Audit logging skipped: {}", e.getMessage());
        }
    }
}
