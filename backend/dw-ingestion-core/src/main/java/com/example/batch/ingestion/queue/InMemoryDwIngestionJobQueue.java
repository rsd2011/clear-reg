package com.example.batch.ingestion.queue;

import java.time.Duration;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.batch.ingestion.DwIngestionService;
import com.example.batch.security.DwBatchAuthContext;
import com.example.dw.application.job.DwIngestionJob;
import com.example.dw.application.job.DwIngestionJobQueue;
import com.example.dw.application.job.DwIngestionJobType;
import com.example.dw.application.job.DwIngestionOutboxService;
import com.example.admin.permission.context.AuthContextPropagator;

@Component
public class InMemoryDwIngestionJobQueue implements DwIngestionJobQueue {

    private static final Logger log = LoggerFactory.getLogger(InMemoryDwIngestionJobQueue.class);

    private final DwIngestionService ingestionService;
    private final DwIngestionOutboxService outboxService;
    private final Executor dwIngestionJobExecutor;
    private final int maxAttempts;
    private final Duration initialBackoff;
    private final double backoffMultiplier;
    private final Duration maxBackoff;

    public InMemoryDwIngestionJobQueue(DwIngestionService ingestionService,
                                       DwIngestionOutboxService outboxService,
                                       @Qualifier("dwIngestionJobExecutor") Executor dwIngestionJobExecutor,
                                       @Value("${dw.ingestion.queue.max-attempts:5}") int maxAttempts,
                                       @Value("${dw.ingestion.queue.backoff.initial-ms:1000}") long initialBackoffMs,
                                       @Value("${dw.ingestion.queue.backoff.multiplier:2.0}") double backoffMultiplier,
                                       @Value("${dw.ingestion.queue.backoff.max-ms:60000}") long maxBackoffMs) {
        this.ingestionService = ingestionService;
        this.outboxService = outboxService;
        this.dwIngestionJobExecutor = dwIngestionJobExecutor;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.initialBackoff = Duration.ofMillis(Math.max(100, initialBackoffMs));
        this.backoffMultiplier = backoffMultiplier <= 1.0 ? 1.0 : backoffMultiplier;
        this.maxBackoff = Duration.ofMillis(Math.max(this.initialBackoff.toMillis(), maxBackoffMs));
    }

    @Override
    public void enqueue(DwIngestionJob job) {
        dwIngestionJobExecutor.execute(() -> handle(job, 1));
    }

    private void handle(DwIngestionJob job, int attempt) {
        try {
            if (job.type() == DwIngestionJobType.FETCH_NEXT) {
                AuthContextPropagator.runWithContext(DwBatchAuthContext.systemContext(), ingestionService::ingestNextFile);
            }
            acknowledge(job);
        }
        catch (Exception exception) {
            handleFailure(job, attempt, exception);
        }
    }

    private void acknowledge(DwIngestionJob job) {
        if (job.hasOutboxReference()) {
            outboxService.markCompleted(job.outboxId());
        }
    }

    private void handleFailure(DwIngestionJob job, int attempt, Exception exception) {
        log.error("DW ingestion job {} failed on attempt {}", job.type(), attempt, exception);
        if (job.hasOutboxReference()) {
            if (attempt >= maxAttempts) {
                outboxService.markDeadLetter(job.outboxId(), exception.getMessage());
                return;
            }
            Duration backoff = computeBackoff(attempt);
            outboxService.scheduleRetry(job.outboxId(), backoff, exception.getMessage());
            return;
        }
        if (attempt >= maxAttempts) {
            return;
        }
        Duration backoff = computeBackoff(attempt);
        try {
            Thread.sleep(backoff.toMillis());
        }
        catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            return;
        }
        handle(job, attempt + 1);
    }

    private Duration computeBackoff(int attempt) {
        double factor = Math.pow(backoffMultiplier, Math.max(0, attempt - 1));
        long calculated = (long) (initialBackoff.toMillis() * factor);
        calculated = Math.max(initialBackoff.toMillis(), calculated);
        calculated = Math.min(calculated, maxBackoff.toMillis());
        return Duration.ofMillis(calculated);
    }
}
