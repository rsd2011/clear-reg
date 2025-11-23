package com.example.audit.infra.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuditKafkaPropertiesTest {

    @Test
    @DisplayName("기본 프로퍼티 값이 설정된다")
    void defaults() {
        AuditKafkaProperties props = new AuditKafkaProperties();
        assertThat(props.getTopic()).isEqualTo("audit.events.v1");
        assertThat(props.getDlqTopic()).isEqualTo("audit.events.dlq");
        assertThat(props.isDlqEnabled()).isFalse();
        assertThat(props.getDlqGroup()).isEqualTo("audit-dlq-reprocessor");
    }

    @Test
    @DisplayName("세터로 값을 덮어쓸 수 있다")
    void overrides() {
        AuditKafkaProperties props = new AuditKafkaProperties();
        props.setTopic("t1");
        props.setDlqTopic("t1.dlq");
        props.setDlqEnabled(true);
        props.setDlqGroup("g1");

        assertThat(props.getTopic()).isEqualTo("t1");
        assertThat(props.getDlqTopic()).isEqualTo("t1.dlq");
        assertThat(props.isDlqEnabled()).isTrue();
        assertThat(props.getDlqGroup()).isEqualTo("g1");
    }
}
