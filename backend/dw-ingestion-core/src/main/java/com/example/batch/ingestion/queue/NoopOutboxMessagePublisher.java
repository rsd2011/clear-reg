package com.example.batch.ingestion.queue;

import org.springframework.stereotype.Component;

import com.example.dw.application.job.OutboxMessagePublisher;
import com.example.dw.domain.DwIngestionOutbox;

/**
 * 브로커 연동 전 단계에서 사용할 No-op Publisher.
 */
@Component
public class NoopOutboxMessagePublisher implements OutboxMessagePublisher {
    @Override
    public void publish(DwIngestionOutbox entry) {
        // no-op
    }
}
