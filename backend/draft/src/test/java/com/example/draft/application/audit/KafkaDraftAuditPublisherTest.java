package com.example.draft.application.audit;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class KafkaDraftAuditPublisherTest {

    @Mock
    KafkaTemplate<String, String> kafkaTemplate;

    @Test
    @DisplayName("Kafka 퍼블리셔는 이벤트를 직렬화해 토픽에 전송한다")
    void publishesToKafka() throws Exception {
        ObjectMapper mapper = org.mockito.Mockito.mock(ObjectMapper.class);
        org.mockito.Mockito.when(mapper.writeValueAsString(isA(DraftAuditEvent.class))).thenReturn("{}");
        KafkaDraftAuditPublisher publisher = new KafkaDraftAuditPublisher(kafkaTemplate, mapper);
        DraftAuditEvent event = sampleEvent();

        publisher.publish(event);

        verify(kafkaTemplate).send(eq("draft-audit-events"), eq(event.draftId().toString()), isA(String.class));
    }

    @Test
    @DisplayName("직렬화 실패 시 IllegalStateException을 던진다")
    void throwsOnSerializeFailure() throws Exception {
        ObjectMapper mapper = org.mockito.Mockito.mock(ObjectMapper.class);
        doThrow(new JsonProcessingException("fail") {}).when(mapper).writeValueAsString(isA(DraftAuditEvent.class));
        KafkaDraftAuditPublisher publisher = new KafkaDraftAuditPublisher(kafkaTemplate, mapper);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> publisher.publish(sampleEvent()))
                .isInstanceOf(IllegalStateException.class);
    }

    private DraftAuditEvent sampleEvent() {
        return new DraftAuditEvent(
                com.example.draft.domain.DraftAction.SUBMITTED,
                UUID.randomUUID(),
                "actor",
                "ORG",
                "comment",
                "127.0.0.1",
                "ua",
                null);
    }
}
