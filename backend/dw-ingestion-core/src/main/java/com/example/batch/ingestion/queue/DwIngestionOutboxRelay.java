package com.example.batch.ingestion.queue;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.dw.application.job.DwIngestionJob;
import com.example.dw.application.job.DwIngestionJobQueue;
import com.example.dw.application.job.DwIngestionOutboxService;
import com.example.dw.application.job.OutboxMessagePublisher;
import com.example.dw.domain.DwIngestionOutbox;

@Component
public class DwIngestionOutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(DwIngestionOutboxRelay.class);

    private final DwIngestionOutboxService outboxService;
    private final DwIngestionJobQueue jobQueue;
    private final OutboxMessagePublisher outboxMessagePublisher;
    private final int batchSize;

    public DwIngestionOutboxRelay(DwIngestionOutboxService outboxService,
                                  DwIngestionJobQueue jobQueue,
                                  OutboxMessagePublisher outboxMessagePublisher,
                                  @Value("${dw.ingestion.outbox.batch-size:25}") int batchSize) {
        this.outboxService = outboxService;
        this.jobQueue = jobQueue;
        this.outboxMessagePublisher = outboxMessagePublisher;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${dw.ingestion.outbox.poll-interval-ms:5000}")
    public void relay() {
        List<DwIngestionOutbox> entries = outboxService.claimPending(batchSize);
        if (entries.isEmpty()) {
            return;
        }
        for (DwIngestionOutbox entry : entries) {
            try {
                // 1단계: in-memory queue 실행
                jobQueue.enqueue(DwIngestionJob.fromOutbox(entry.getId(), entry.getJobType()));
                // 2단계: 브로커 퍼블리셔 (SQS/Kafka) 후속 전환 대비
                if (outboxMessagePublisher != null) {
                    outboxMessagePublisher.publish(entry);
                }
            }
            catch (Exception ex) {
                log.error("Failed to relay DW ingestion job {}", entry.getId(), ex);
                outboxService.markFailed(entry);
            }
        }
    }
}
