package com.example.batch.ingestion.queue;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;

import com.example.dw.domain.DwIngestionOutbox;
import com.example.dw.application.job.DwIngestionJobType;
import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("unchecked")
class KafkaOutboxMessagePublisherFailureTest {

    KafkaTemplate<String, String> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Kafka send 실패 시 예외가 전파된다")
    void sendFailure_propagates() {
        KafkaOutboxMessagePublisher publisher = new KafkaOutboxMessagePublisher(kafkaTemplate, objectMapper, "topic");
        DwIngestionOutbox outbox = DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, java.time.Clock.systemUTC());
        outbox.withPayload("{\"payload\":\"value\"}");
        given(kafkaTemplate.send(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .willThrow(new RuntimeException("send fail"));

        assertThatThrownBy(() -> publisher.publish(outbox))
                .isInstanceOf(RuntimeException.class);
    }
}
