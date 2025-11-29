package com.example.common.masking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.policy.PolicyToggleSettings;

@DisplayName("마스킹 전략 분기 커버리지")
class MaskingStrategyBranchCoverageTest {

    @Test
    @DisplayName("기본 apply 분기: forceUnmask/종류/필드")
    void applyBranches() {
        MaskingTarget forceAll = MaskingTarget.builder()
                .subjectType(SubjectType.CUSTOMER_INDIVIDUAL)
                .forceUnmask(true)
                .build();

        MaskingTarget forceKind = MaskingTarget.builder()
                .subjectType(SubjectType.CUSTOMER_INDIVIDUAL)
                .dataKind(DataKind.SSN)
                .forceUnmaskKinds(Set.of(DataKind.SSN))
                .build();

        MaskingTarget forceField = MaskingTarget.builder()
                .subjectType(SubjectType.CUSTOMER_INDIVIDUAL)
                .forceUnmaskFields(Set.of("name"))
                .build();

        MaskingTarget normal = MaskingTarget.builder()
                .subjectType(SubjectType.CUSTOMER_INDIVIDUAL)
                .defaultMask(true)
                .build();

        MaskingStrategy strategy = target -> true; // 항상 마스킹하는 전략

        assertThat(strategy.apply("raw", forceAll, "****", "name")).isEqualTo("raw");
        assertThat(strategy.apply("raw", forceKind, "****", "name")).isEqualTo("raw");
        assertThat(strategy.apply("raw", forceField, "****", "name")).isEqualTo("raw");
        assertThat(strategy.apply("raw", normal, "****", "name")).isEqualTo("****");

        // 마스킹 비활성화 시
        MaskingStrategy noMask = target -> false;
        assertThat(noMask.apply("raw", normal, "****", "name")).isEqualTo("raw");

        // raw null 처리 브랜치
        assertThat(strategy.apply(null, normal, "****", "name")).isNull();
    }

    @Test
    @DisplayName("PolicyMaskingStrategy: 주체별 기본/롤 기반 해제")
    void policyStrategyBranches() {
        PolicyToggleSettings settings = new PolicyToggleSettings(
                true, true, true,
                java.util.List.of(), 20 * 1024 * 1024L, java.util.List.of(), true, 365,
                true, true, true, 730, true, "MEDIUM", true, java.util.List.of(), java.util.List.of());
        PolicyMaskingStrategy strategy = new PolicyMaskingStrategy(settings,
                java.util.Map.of(SubjectType.CUSTOMER_INDIVIDUAL, true,
                        SubjectType.EMPLOYEE, false),
                Set.of("AUDIT_ADMIN"));

        MaskingTarget customer = MaskingTarget.builder()
                .subjectType(SubjectType.CUSTOMER_INDIVIDUAL)
                .defaultMask(true)
                .requesterRoles(Set.of("USER"))
                .build();
        assertThat(strategy.shouldMask(customer)).isTrue();

        MaskingTarget employee = MaskingTarget.builder()
                .subjectType(SubjectType.EMPLOYEE)
                .defaultMask(true)
                .build();
        assertThat(strategy.shouldMask(employee)).isFalse();

        MaskingTarget adminForce = MaskingTarget.builder()
                .subjectType(SubjectType.CUSTOMER_INDIVIDUAL)
                .requesterRoles(Set.of("AUDIT_ADMIN"))
                .forceUnmask(true)
                .build();
        assertThat(strategy.shouldMask(adminForce)).isFalse();

        // target null 시 기본값 확인
        assertThat(strategy.shouldMask(null)).isTrue();
    }
}
