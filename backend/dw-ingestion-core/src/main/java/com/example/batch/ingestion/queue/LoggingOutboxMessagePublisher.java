package com.example.batch.ingestion.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.example.dw.application.job.OutboxMessagePublisher;
import com.example.dw.domain.DwIngestionOutbox;

/**
 * 단순 로깅 기반 Outbox 퍼블리셔. 브로커 연동 전 단계에서 페이로드 직렬화 없이 이벤트 흐름을 검증할 때 사용한다.
 */
@Component
@ConditionalOnProperty(prefix = "dw.ingestion.outbox.publisher", name = "type", havingValue = "log")
public class LoggingOutboxMessagePublisher implements OutboxMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingOutboxMessagePublisher.class);

    @Override
    public void publish(DwIngestionOutbox entry) {
        log.info("Publishing outbox entry id={}, type={}, payload={}", entry.getId(), entry.getJobType(), entry.getPayload());
    }
}
