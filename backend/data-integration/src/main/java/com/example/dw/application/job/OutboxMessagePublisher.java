package com.example.dw.application.job;

import com.example.dw.domain.DwIngestionOutbox;

/**
 * 외부 브로커(SQS/Kafka 등)에 Outbox 레코드를 퍼블리시하기 위한 포트.
 */
public interface OutboxMessagePublisher {

    /**
     * Outbox 레코드를 메시지로 직렬화하여 브로커에 발행한다.
     *
     * @param entry Outbox 엔트리
     */
    void publish(DwIngestionOutbox entry);
}
