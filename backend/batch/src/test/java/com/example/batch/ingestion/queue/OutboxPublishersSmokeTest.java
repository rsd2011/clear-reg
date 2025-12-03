package com.example.batch.ingestion.queue;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import com.example.dw.application.job.DwIngestionJobType;
import com.example.dw.domain.DwIngestionOutbox;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

@ExtendWith(MockitoExtension.class)
class OutboxPublishersSmokeTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2024-02-01T00:00:00Z"), ZoneOffset.UTC);
    private static final DwIngestionOutbox OUTBOX = DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, CLOCK);

    @Mock
    KafkaTemplate<String, String> kafkaTemplate;

    @Test
    @DisplayName("Noop 퍼블리셔는 예외 없이 종료한다")
    void noopPublisherDoesNothing() {
        NoopOutboxMessagePublisher publisher = new NoopOutboxMessagePublisher();
        assertThatNoException().isThrownBy(() -> publisher.publish(OUTBOX));
    }

    @Test
    @DisplayName("로깅 퍼블리셔는 예외 없이 로그만 남긴다")
    void loggingPublisher() {
        LoggingOutboxMessagePublisher publisher = new LoggingOutboxMessagePublisher();
        assertThatNoException().isThrownBy(() -> publisher.publish(OUTBOX));
    }

    @Test
    @DisplayName("Kafka 퍼블리셔는 토픽에 이벤트를 전송한다")
    void kafkaPublisherSends() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        KafkaOutboxMessagePublisher publisher = new KafkaOutboxMessagePublisher(kafkaTemplate, mapper, "topic");

        assertThatNoException().isThrownBy(() -> publisher.publish(OUTBOX));
        verify(kafkaTemplate).send(org.mockito.ArgumentMatchers.eq("topic"),
                org.mockito.ArgumentMatchers.eq(OUTBOX.getId().toString()),
                org.mockito.ArgumentMatchers.any(String.class));
    }

    @Test
    @DisplayName("Kafka 퍼블리셔 전송 실패 시 예외를 전파한다")
    void kafkaPublisherPropagatesException() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        KafkaOutboxMessagePublisher publisher = new KafkaOutboxMessagePublisher(kafkaTemplate, mapper, "topic");
        org.mockito.Mockito.doThrow(new IllegalStateException("kafka down"))
                .when(kafkaTemplate).send(org.mockito.ArgumentMatchers.eq("topic"),
                        org.mockito.ArgumentMatchers.eq(OUTBOX.getId().toString()),
                        org.mockito.ArgumentMatchers.any(String.class));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> publisher.publish(OUTBOX))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("kafka down");
    }
}
