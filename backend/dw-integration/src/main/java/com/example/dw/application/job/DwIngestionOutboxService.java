package com.example.dw.application.job;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dw.domain.DwIngestionOutbox;
import com.example.dw.domain.DwIngestionOutboxRepository;

@Service
public class DwIngestionOutboxService {

    private final DwIngestionOutboxRepository repository;
    private final Clock clock;

    public DwIngestionOutboxService(DwIngestionOutboxRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void enqueue(DwIngestionJob job) {
        DwIngestionOutbox entry = DwIngestionOutbox.pending(job.type(), clock);
        if (job.payload() != null) {
            entry.setPayload(job.payload());
        }
        repository.save(entry);
    }

    @Transactional
    public List<DwIngestionOutbox> claimPending(int batchSize) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<DwIngestionOutbox> pending = repository
                .lockOldestPending(DwIngestionOutboxStatus.PENDING, now, org.springframework.data.domain.PageRequest.of(0, batchSize));
        pending.forEach(entry -> entry.markSending(clock, "dw-outbox-relay"));
        return pending;
    }

    @Transactional
    public void markCompleted(UUID entryId) {
        updateEntry(entryId, entity -> entity.markSent(clock));
    }

    @Transactional
    public void markFailed(DwIngestionOutbox entry) {
        entry.markFailed(clock);
        repository.save(entry);
    }

    @Transactional
    public void markFailed(UUID entryId, String errorMessage) {
        updateEntry(entryId, entity -> entity.markFailed(clock, errorMessage));
    }

    @Transactional
    public void scheduleRetry(UUID entryId, Duration delay, String lastErrorMessage) {
        updateEntry(entryId, entity -> entity.markRetry(clock, delay, lastErrorMessage));
    }

    @Transactional
    public void markDeadLetter(UUID entryId, String lastErrorMessage) {
        updateEntry(entryId, entity -> entity.markDeadLetter(clock, lastErrorMessage));
    }

    private void updateEntry(UUID entryId, Consumer<DwIngestionOutbox> consumer) {
        Optional<DwIngestionOutbox> entry = repository.findById(entryId);
        entry.ifPresent(entity -> {
            consumer.accept(entity);
            repository.save(entity);
        });
    }
}
