package com.example.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PolicyDocumentEntityTest {

    @Test
    @DisplayName("yaml 업데이트 시 updatedAt이 갱신된다")
    void updateYaml_updatesTimestamp() {
        PolicyDocument doc = new PolicyDocument("CODE", "initial");
        Instant before = doc.getUpdatedAt();

        doc.updateYaml("next");

        assertThat(doc.getYaml()).isEqualTo("next");
        assertThat(doc.getUpdatedAt()).isAfter(before);
    }
}
