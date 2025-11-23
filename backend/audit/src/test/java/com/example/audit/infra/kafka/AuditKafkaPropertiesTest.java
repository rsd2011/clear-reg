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

    @Test
    @DisplayName("equals/hashCode/toString 브랜치를 모두 커버한다")
    void equalsHashCodeToString() {
        AuditKafkaProperties a = new AuditKafkaProperties();
        a.setTopic("t");
        a.setDlqTopic("dlq");
        a.setDlqEnabled(true);
        a.setDlqGroup("g");

        AuditKafkaProperties b = new AuditKafkaProperties();
        b.setTopic("t");
        b.setDlqTopic("dlq");
        b.setDlqEnabled(true);
        b.setDlqGroup("g");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.toString()).contains("t").contains("dlq").contains("g");

        // branch: null, other type, field difference
        assertThat(a.equals(null)).isFalse();
        assertThat(a.equals("string")).isFalse();
        b.setDlqEnabled(false);
        assertThat(a).isNotEqualTo(b);
        assertThat(a.canEqual("string")).isFalse();

        assertThat(a).isEqualTo(a); // self branch
    }

    @Test
    @DisplayName("equals는 null 필드 조합을 모두 처리한다")
    void equalsHandlesNullFields() {
        AuditKafkaProperties left = new AuditKafkaProperties();
        left.setTopic(null);
        left.setDlqTopic("dlq");
        left.setDlqEnabled(false);
        left.setDlqGroup(null);

        AuditKafkaProperties right = new AuditKafkaProperties();
        right.setTopic(null);
        right.setDlqTopic("dlq");
        right.setDlqEnabled(false);
        right.setDlqGroup(null);

        assertThat(left).isEqualTo(right);          // both null -> equal

        right.setDlqTopic(null);                    // mismatch null/not-null
        assertThat(left).isNotEqualTo(right);
    }

    @Test
    @DisplayName("각 필드가 다르면 equals가 false를 반환한다")
    void equalsDetectsFieldDifferences() {
        AuditKafkaProperties base = new AuditKafkaProperties();
        base.setTopic("t0");
        base.setDlqTopic("d0");
        base.setDlqEnabled(false);
        base.setDlqGroup("g0");

        AuditKafkaProperties diffTopic = new AuditKafkaProperties();
        diffTopic.setTopic("t1");
        diffTopic.setDlqTopic("d0");
        diffTopic.setDlqEnabled(false);
        diffTopic.setDlqGroup("g0");
        assertThat(base).isNotEqualTo(diffTopic);

        AuditKafkaProperties diffDlqTopic = new AuditKafkaProperties();
        diffDlqTopic.setTopic("t0");
        diffDlqTopic.setDlqTopic("d1");
        diffDlqTopic.setDlqEnabled(false);
        diffDlqTopic.setDlqGroup("g0");
        assertThat(base).isNotEqualTo(diffDlqTopic);

        AuditKafkaProperties diffGroup = new AuditKafkaProperties();
        diffGroup.setTopic("t0");
        diffGroup.setDlqTopic("d0");
        diffGroup.setDlqEnabled(false);
        diffGroup.setDlqGroup("g1");
        assertThat(base).isNotEqualTo(diffGroup);
    }
}
