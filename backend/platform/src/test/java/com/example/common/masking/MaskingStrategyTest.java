package com.example.common.masking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.policy.PolicyToggleSettings;

class MaskingStrategyTest {

    @Test
    @DisplayName("Customer is masked, employee not masked when policy enables masking")
    void maskingBySubjectType() {
        PolicyToggleSettings settings = new PolicyToggleSettings(true, true, true,
                java.util.List.of("PASSWORD"), 20L, java.util.List.of("pdf"), true, 365,
                true, true, true, 730, true, "MEDIUM", true, java.util.List.of(), java.util.List.of());
        java.util.Map<SubjectType, Boolean> customRules = java.util.Map.of(
                SubjectType.CUSTOMER_INDIVIDUAL, true,
                SubjectType.CUSTOMER_CORPORATE, true,
                SubjectType.EMPLOYEE, false,
                SubjectType.SYSTEM, false,
                SubjectType.UNKNOWN, true);
        PolicyMaskingStrategy strategy = new PolicyMaskingStrategy(settings, customRules, java.util.Set.of());

        MaskingTarget customer = MaskingTarget.builder().subjectType(SubjectType.CUSTOMER_INDIVIDUAL).defaultMask(true).build();
        MaskingTarget employee = MaskingTarget.builder().subjectType(SubjectType.EMPLOYEE).defaultMask(true).build();

        assertThat(strategy.shouldMask(customer)).isTrue();
        assertThat(strategy.shouldMask(employee)).isFalse();
    }

    @Test
    @DisplayName("Custom rule can enable masking for system subjects")
    void customRuleForSystem() {
        PolicyToggleSettings settings = new PolicyToggleSettings(true, true, true,
                java.util.List.of("PASSWORD"), 20L, java.util.List.of("pdf"), true, 365);
        java.util.Map<SubjectType, Boolean> rules = new java.util.EnumMap<>(SubjectType.class);
        rules.put(SubjectType.SYSTEM, true);
        PolicyMaskingStrategy strategy = new PolicyMaskingStrategy(settings, rules, java.util.Set.of());

        MaskingTarget system = MaskingTarget.builder().subjectType(SubjectType.SYSTEM).defaultMask(false).build();
        assertThat(strategy.shouldMask(system)).isTrue();
    }

    @Test
    @DisplayName("Allowed roles can unmask when rules permit")
    void allowedRolesUnmask() {
        PolicyToggleSettings settings = new PolicyToggleSettings(true, true, true,
                java.util.List.of("PASSWORD"), 20L, java.util.List.of("pdf"), true, 365,
                true, true, true, 730, true, "MEDIUM", true, java.util.List.of(), java.util.List.of());
        java.util.Map<SubjectType, Boolean> rules = new java.util.EnumMap<>(SubjectType.class);
        rules.put(SubjectType.CUSTOMER_INDIVIDUAL, true);
        rules.put(SubjectType.CUSTOMER_CORPORATE, true);
        rules.put(SubjectType.EMPLOYEE, false);
        rules.put(SubjectType.SYSTEM, false);
        rules.put(SubjectType.UNKNOWN, true);
        java.util.Set<String> allowed = java.util.Set.of("AUDIT_ADMIN");
        PolicyMaskingStrategy strategy = new PolicyMaskingStrategy(settings, rules, allowed);

        MaskingTarget target = MaskingTarget.builder()
                .subjectType(SubjectType.CUSTOMER_INDIVIDUAL)
                .defaultMask(true)
                .forceUnmaskKinds(java.util.Set.of("RRN"))
                .dataKind("RRN")
                .requesterRoles(java.util.Set.of("AUDIT_ADMIN"))
                .build();

        assertThat(strategy.apply("123456-1234567", target, "[MASKED]")).isEqualTo("123456-1234567");
    }
}
