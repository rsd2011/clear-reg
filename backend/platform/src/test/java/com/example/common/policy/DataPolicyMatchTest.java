package com.example.common.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DataPolicyMatchTest {

    @Test
    @DisplayName("DataPolicyMatch 빌더로 모든 필드를 설정할 수 있다")
    void builderCoversFields() {
        UUID id = UUID.randomUUID();
        DataPolicyMatch match = DataPolicyMatch.builder()
                .policyId(id)
                .rowScope(com.example.common.security.RowScope.ORG)
                .maskRule("PARTIAL")
                .maskParams("{\"k\":\"v\"}")
                .priority(1)
                .build();

        assertThat(match.getPolicyId()).isEqualTo(id);
        assertThat(match.getMaskRule()).isEqualTo("PARTIAL");
        assertThat(match.getPriority()).isEqualTo(1);
    }
}
