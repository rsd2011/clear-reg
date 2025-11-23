package com.example.audit.infra.kafka;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

class AuditDlqReprocessorSuccessTest {

    @Test
    @DisplayName("DLQ 이벤트를 정상 토픽으로 재전송한다")
    void requeueSuccess() {
        KafkaTemplate<String, String> template = org.mockito.Mockito.mock(KafkaTemplate.class);
        AuditKafkaProperties props = new AuditKafkaProperties();
        props.setTopic("audit.events.v1");

        AuditDlqReprocessor reprocessor = new AuditDlqReprocessor(template, props);
        reprocessor.handleDlq("payload", "event-key");

        ArgumentCaptor<String> topic = ArgumentCaptor.forClass(String.class);
        verify(template).send(topic.capture(), org.mockito.Mockito.eq("event-key"), org.mockito.Mockito.eq("payload"));
    }
}
