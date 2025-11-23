package com.example.audit.infra.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuditKafkaPropertiesBranchTest {

    @Test
    @DisplayName("dlqEnabled가 다르면 equals가 false")
    void equalsFalseWhenDlqEnabledDiffers() {
        AuditKafkaProperties a = new AuditKafkaProperties();
        AuditKafkaProperties b = new AuditKafkaProperties();
        b.setDlqEnabled(true);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("동일 필드면 equals/hashCode가 일치한다")
    void equalsTrueWhenSame() {
        AuditKafkaProperties a = new AuditKafkaProperties();
        AuditKafkaProperties b = new AuditKafkaProperties();
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
