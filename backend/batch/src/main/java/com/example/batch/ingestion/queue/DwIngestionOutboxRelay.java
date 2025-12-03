package com.example.batch.ingestion.queue;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.ObjectProvider;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.schedule.BatchJobCode;
import com.example.common.schedule.BatchJobDefaults;
import com.example.common.schedule.BatchJobSchedule;
import com.example.common.schedule.ScheduledJobPort;
import com.example.common.schedule.TriggerDescriptor;
import com.example.dw.application.job.DwIngestionJob;
import com.example.dw.application.job.DwIngestionJobQueue;
import com.example.dw.application.job.DwIngestionOutboxService;
import com.example.dw.application.job.OutboxMessagePublisher;
import com.example.dw.domain.DwIngestionOutbox;

@Component
public class DwIngestionOutboxRelay implements ScheduledJobPort {

    private static final Logger log = LoggerFactory.getLogger(DwIngestionOutboxRelay.class);

    private final DwIngestionOutboxService outboxService;
    private final DwIngestionJobQueue jobQueue;
    private final OutboxMessagePublisher outboxMessagePublisher;
    private final int batchSize;
    private final long pollIntervalMs;
    private final PolicySettingsProvider policySettingsProvider;
    private final boolean centralSchedulerEnabled;

    public DwIngestionOutboxRelay(DwIngestionOutboxService outboxService,
                                  DwIngestionJobQueue jobQueue,
                                  OutboxMessagePublisher outboxMessagePublisher,
                                  @Value("${dw.ingestion.outbox.batch-size:25}") int batchSize,
                                  @Value("${dw.ingestion.outbox.poll-interval-ms:5000}") long pollIntervalMs,
                                  ObjectProvider<PolicySettingsProvider> policySettingsProvider,
                                  @Value("${central.scheduler.enabled:false}") boolean centralSchedulerEnabled) {
        this.outboxService = outboxService;
        this.jobQueue = jobQueue;
        this.outboxMessagePublisher = outboxMessagePublisher;
        this.batchSize = batchSize;
        this.pollIntervalMs = pollIntervalMs;
        this.policySettingsProvider = policySettingsProvider.getIfAvailable();
        this.centralSchedulerEnabled = centralSchedulerEnabled;
    }

    // 단축 생성자 제거(테스트는 ObjectProvider mock 사용)

    @Scheduled(fixedDelayString = "${dw.ingestion.outbox.poll-interval-ms:5000}")
    public void relay() {
        if (centralSchedulerEnabled) {
            return;
        }
        runOnce(java.time.Instant.now());
    }

    @Override
    public String jobId() {
        return BatchJobCode.DW_INGESTION_OUTBOX_RELAY.name();
    }

    @Override
    public TriggerDescriptor trigger() {
        return resolveSchedule().toTriggerDescriptor();
    }

    @Override
    public void runOnce(java.time.Instant now) {
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

    private BatchJobSchedule resolveSchedule() {
        BatchJobSchedule policy = policySettingsProvider == null ? null
                : policySettingsProvider.batchJobSchedule(BatchJobCode.DW_INGESTION_OUTBOX_RELAY);
        if (policy != null) {
            return policy;
        }
        long interval = pollIntervalMs > 0 ? pollIntervalMs
                : BatchJobDefaults.defaults().get(BatchJobCode.DW_INGESTION_OUTBOX_RELAY).fixedDelayMillis();
        return new BatchJobSchedule(true, com.example.common.schedule.TriggerType.FIXED_DELAY, null, interval, 0, null);
    }
}
