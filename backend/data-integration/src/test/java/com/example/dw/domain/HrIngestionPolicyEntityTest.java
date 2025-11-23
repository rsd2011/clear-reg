package com.example.dw.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HrIngestionPolicyEntityTest {

    @Test
    @DisplayName("정책 엔티티는 YAML 업데이트 시 updatedAt을 갱신한다")
    void updateYamlUpdatesTimestamp() {
        HrIngestionPolicyEntity entity = new HrIngestionPolicyEntity("code", "yaml1");
        Instant before = entity.getUpdatedAt();

        entity.updateYaml("yaml2");

        assertThat(entity.getYaml()).isEqualTo("yaml2");
        assertThat(entity.getUpdatedAt()).isAfter(before);
    }
}
