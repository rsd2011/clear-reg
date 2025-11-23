package com.example.audit.infra.kafka;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

class AuditDlqReprocessorBranchTest {

    @Test
    @DisplayName("DLQ 재전송 시 예외가 나도 삼키고 로그만 남긴다")
    void requeueHandlesException() {
        KafkaTemplate<String, String> template = org.mockito.Mockito.mock(KafkaTemplate.class);
        AuditKafkaProperties props = new AuditKafkaProperties();
        props.setTopic("audit.events.v1");
        doThrow(new RuntimeException("broker down")).when(template).send(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyString(), org.mockito.Mockito.anyString());

        AuditDlqReprocessor reprocessor = new AuditDlqReprocessor(template, props);
        reprocessor.handleDlq("payload", "event-key");

        ArgumentCaptor<String> topic = ArgumentCaptor.forClass(String.class);
        verify(template).send(topic.capture(), org.mockito.Mockito.eq("event-key"), org.mockito.Mockito.eq("payload"));
    }
}
