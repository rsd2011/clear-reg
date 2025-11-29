package com.example.audit.infra.kafka;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;

@SuppressWarnings("unchecked")
class AuditDlqReprocessorTest {

    @Test
    @DisplayName("DLQ 이벤트를 주 토픽으로 재전송한다")
    void requeueToMainTopic() {
        KafkaTemplate<String, String> template = Mockito.mock(KafkaTemplate.class);
        AuditKafkaProperties props = new AuditKafkaProperties();
        props.setTopic("audit.events.v1");
        AuditDlqReprocessor reprocessor = new AuditDlqReprocessor(template, props);

        reprocessor.handleDlq("{\"event\":\"x\"}", "k1");

        verify(template).send("audit.events.v1", "k1", "{\"event\":\"x\"}");
    }

    @Test
    @DisplayName("전송 실패 시 예외를 던지지 않고 로그만 남긴다")
    void requeueFailureIsSwallowed() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> template = Mockito.mock(KafkaTemplate.class);
        Mockito.doThrow(new RuntimeException("fail")).when(template).send(Mockito.any(), Mockito.any(), Mockito.any());
        AuditKafkaProperties props = new AuditKafkaProperties();
        AuditDlqReprocessor reprocessor = new AuditDlqReprocessor(template, props);

        // should not throw
        reprocessor.handleDlq("{\"event\":\"x\"}", "k1");
    }

    @Test
    @DisplayName("key가 null이면 빈 문자열로 대체된다")
    void nullKeyReplacedWithEmpty() {
        KafkaTemplate<String, String> template = Mockito.mock(KafkaTemplate.class);
        AuditKafkaProperties props = new AuditKafkaProperties();
        props.setTopic("audit.events.v1");
        AuditDlqReprocessor reprocessor = new AuditDlqReprocessor(template, props);

        reprocessor.handleDlq("{\"event\":\"x\"}", null);

        // null key는 빈 문자열로 변환됨
        verify(template).send("audit.events.v1", "", "{\"event\":\"x\"}");
    }
}
