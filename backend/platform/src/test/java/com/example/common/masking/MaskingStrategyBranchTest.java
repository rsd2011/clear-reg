package com.example.common.masking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.policy.PolicyToggleSettings;

class MaskingStrategyBranchTest {

    @Test
    @DisplayName("SubjectType 미지정 시 기본값을 따른다")
    void defaultWhenSubjectUnknown() {
        PolicyToggleSettings settings = new PolicyToggleSettings(true, true, true, java.util.List.of(), 1024, java.util.List.of(), true, 30);
        MaskingStrategy strategy = new PolicyMaskingStrategy(settings);
        MaskingTarget target = MaskingTarget.builder().defaultMask(true).build();
        assertThat(strategy.shouldMask(target)).isTrue();
    }

    @Test
    @DisplayName("허용된 역할이 없으면 forceUnmask도 차단된다")
    void disallowForceUnmaskWithoutRole() {
        PolicyToggleSettings settings = new PolicyToggleSettings(true, true, true, java.util.List.of(), 1024, java.util.List.of(), true, 30);
        MaskingStrategy strategy = new PolicyMaskingStrategy(settings,
                Map.of(SubjectType.CUSTOMER_CORPORATE, true),
                Set.of("ROLE_ALLOWED"));

        MaskingTarget target = MaskingTarget.builder()
                .subjectType(SubjectType.CUSTOMER_CORPORATE)
                .defaultMask(true)
                .forceUnmask(true)
                .requesterRoles(Set.of("OTHER"))
                .build();

        assertThat(strategy.shouldMask(target)).isTrue();
    }
}
