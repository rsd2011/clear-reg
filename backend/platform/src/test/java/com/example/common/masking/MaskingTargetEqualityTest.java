package com.example.common.masking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MaskingTarget 상세 equals 브랜치")
class MaskingTargetEqualityTest {

    @Test
    @DisplayName("필드별 불일치 분기 커버")
    void equalsBranches() {
        MaskingTarget base = MaskingTarget.builder()
                .subjectType(SubjectType.CUSTOMER_INDIVIDUAL)
                .dataKind("RRN")
                .defaultMask(true)
                .forceUnmask(false)
                .forceUnmaskKinds(Set.of("RRN"))
                .forceUnmaskFields(Set.of("name"))
                .requesterRoles(Set.of("AUDIT_ADMIN"))
                .rowId("row-1")
                .maskRule("FULL")
                .maskParams("{}").build();

        assertThat(base).isEqualTo(MaskingTarget.builder()
                .subjectType(SubjectType.CUSTOMER_INDIVIDUAL).dataKind("RRN").defaultMask(true).forceUnmask(false)
                .forceUnmaskKinds(Set.of("RRN")).forceUnmaskFields(Set.of("name")).requesterRoles(Set.of("AUDIT_ADMIN"))
                .rowId("row-1").maskRule("FULL").maskParams("{}").build());

        assertThat(base).isNotEqualTo(builderFromBase().subjectType(SubjectType.EMPLOYEE).build());
        assertThat(base).isNotEqualTo(builderFromBase().dataKind("CARD").build());
        assertThat(base).isNotEqualTo(builderFromBase().defaultMask(false).build());
        assertThat(base).isNotEqualTo(builderFromBase().forceUnmask(true).build());
        assertThat(base).isNotEqualTo(builderFromBase().forceUnmaskKinds(Set.of("CARD")).build());
        assertThat(base).isNotEqualTo(builderFromBase().forceUnmaskFields(Set.of("addr")).build());
        assertThat(base).isNotEqualTo(builderFromBase().requesterRoles(Set.of("USER")).build());
        assertThat(base).isNotEqualTo(builderFromBase().rowId("row-2").build());
        assertThat(base).isNotEqualTo(builderFromBase().maskRule("HASH").build());
        assertThat(base).isNotEqualTo(builderFromBase().maskParams("{p}").build());

        MaskingTarget nullFields = MaskingTarget.builder().build();
        assertThat(nullFields.hashCode()).isNotZero();
    }

    private MaskingTarget.MaskingTargetBuilder builderFromBase() {
        return MaskingTarget.builder()
                .subjectType(SubjectType.CUSTOMER_INDIVIDUAL)
                .dataKind("RRN")
                .defaultMask(true)
                .forceUnmask(false)
                .forceUnmaskKinds(Set.of("RRN"))
                .forceUnmaskFields(Set.of("name"))
                .requesterRoles(Set.of("AUDIT_ADMIN"))
                .rowId("row-1")
                .maskRule("FULL")
                .maskParams("{}");
    }
}
