package com.example.common.masking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.policy.PolicyToggleSettings;

class MaskingStrategyPlatformBranchesTest {

    @Test
    @DisplayName("defaultMask=false면 subjectType 미지정 시 마스킹하지 않는다")
    void defaultMaskFalse() {
        PolicyToggleSettings settings = new PolicyToggleSettings(false, true, true, java.util.List.of(), 1024, java.util.List.of(), true, 30);
        MaskingStrategy strategy = new PolicyMaskingStrategy(settings, Map.of(), java.util.Set.of());
        MaskingTarget target = MaskingTarget.builder()
                .defaultMask(false)
                .dataKind("GEN")
                .build();
        assertThat(strategy.shouldMask(target)).isFalse();
    }
}
