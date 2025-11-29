package com.example.common.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MaskingContextHolderTest {

    @AfterEach
    void cleanup() {
        MaskingContextHolder.clear();
    }

    @Test
    @DisplayName("Given: MaskingMatch 설정 / When: get() 호출 / Then: 동일한 매치 반환")
    void setAndGet() {
        MaskingMatch match = MaskingMatch.builder()
                .policyId(UUID.randomUUID())
                .maskRule("PARTIAL")
                .maskParams("{}")
                .auditEnabled(true)
                .priority(1)
                .build();

        MaskingContextHolder.set(match);

        assertThat(MaskingContextHolder.get()).isEqualTo(match);
    }

    @Test
    @DisplayName("Given: MaskingMatch 설정 후 clear / When: get() 호출 / Then: null 반환")
    void clearRemovesMatch() {
        MaskingMatch match = MaskingMatch.builder()
                .maskRule("FULL")
                .build();

        MaskingContextHolder.set(match);
        MaskingContextHolder.clear();

        assertThat(MaskingContextHolder.get()).isNull();
    }

    @Test
    @DisplayName("Given: 아무것도 설정하지 않음 / When: get() 호출 / Then: null 반환")
    void getReturnsNullWhenNotSet() {
        assertThat(MaskingContextHolder.get()).isNull();
    }
}
