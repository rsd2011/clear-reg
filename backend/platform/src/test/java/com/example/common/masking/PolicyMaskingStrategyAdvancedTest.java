package com.example.common.masking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.example.common.policy.PolicyToggleSettings;

class PolicyMaskingStrategyAdvancedTest {

    @Test
    void allowsForceUnmaskWhenRoleAuthorized() {
        PolicyToggleSettings settings = new PolicyToggleSettings(
                true, true, true, java.util.List.of(), 20 * 1024 * 1024,
                java.util.List.of(), true, 365);
        PolicyMaskingStrategy strategy = new PolicyMaskingStrategy(
                settings,
                Map.of(SubjectType.CUSTOMER_INDIVIDUAL, true, SubjectType.EMPLOYEE, false),
                Set.of("AUDIT_ADMIN"));

        MaskingTarget target = MaskingTarget.builder()
                .subjectType(SubjectType.CUSTOMER_INDIVIDUAL)
                .dataKind(DataKind.SSN)
                .defaultMask(true)
                .forceUnmask(false)
                .forceUnmaskKinds(Set.of(DataKind.SSN))
                .requesterRoles(Set.of("AUDIT_ADMIN"))
                .build();

        assertThat(strategy.shouldMask(target)).isFalse();
    }

    @Test
    void employeeIsUnmaskedByDefaultEvenIfGlobalMaskEnabled() {
        PolicyToggleSettings settings = new PolicyToggleSettings(
                true, true, true, java.util.List.of(), 20 * 1024 * 1024,
                java.util.List.of(), true, 365);
        PolicyMaskingStrategy strategy = new PolicyMaskingStrategy(settings);

        MaskingTarget target = MaskingTarget.builder()
                .subjectType(SubjectType.EMPLOYEE)
                .defaultMask(true)
                .build();

        assertThat(strategy.shouldMask(target)).isFalse();
    }

    @Test
    void forceUnmaskFieldsByAuthorizedRoleDisablesMasking() {
        PolicyToggleSettings settings = new PolicyToggleSettings(
                true, true, true, java.util.List.of(), 20 * 1024 * 1024,
                java.util.List.of(), true, 365);
        PolicyMaskingStrategy strategy = new PolicyMaskingStrategy(
                settings,
                Map.of(SubjectType.CUSTOMER_CORPORATE, true),
                Set.of("COMPLIANCE"));

        MaskingTarget target = MaskingTarget.builder()
                .subjectType(SubjectType.CUSTOMER_CORPORATE)
                .defaultMask(true)
                .forceUnmaskFields(Set.of("name"))
                .requesterRoles(Set.of("COMPLIANCE"))
                .build();

        assertThat(strategy.shouldMask(target)).isFalse();
    }
}
