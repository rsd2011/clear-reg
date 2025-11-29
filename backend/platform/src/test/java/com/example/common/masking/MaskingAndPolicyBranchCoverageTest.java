package com.example.common.masking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import com.example.common.policy.RowAccessMatch;
import com.example.common.security.RowScope;
import com.example.common.security.RowScopeContext;
import com.example.common.security.RowScopeEvaluator;
import com.example.common.security.RowScopeFilter;
import com.example.common.security.RowScopeSpecifications;

@DisplayName("마스킹/정책 브랜치 커버리지")
class MaskingAndPolicyBranchCoverageTest {

    @Test
    @DisplayName("MaskingTarget/UnmaskAuditEvent 동등성·빌더 브랜치")
    void maskingTargetEquality() {
        MaskingTarget t1 = MaskingTarget.builder()
                .subjectType(SubjectType.CUSTOMER_INDIVIDUAL)
                .dataKind("RRN")
                .defaultMask(true)
                .forceUnmask(false)
                .forceUnmaskKinds(Set.of("RRN"))
                .forceUnmaskFields(Set.of("name"))
                .requesterRoles(Set.of("AUDIT_ADMIN"))
                .rowId("row-1")
                .maskRule("FULL")
                .maskParams("{}")
                .build();
        MaskingTarget t2 = MaskingTarget.builder().subjectType(SubjectType.CUSTOMER_INDIVIDUAL).dataKind("RRN")
                .defaultMask(true).forceUnmask(false).forceUnmaskKinds(Set.of("RRN"))
                .forceUnmaskFields(Set.of("name")).requesterRoles(Set.of("AUDIT_ADMIN")).rowId("row-1")
                .maskRule("FULL").maskParams("{}").build();
        MaskingTarget different = MaskingTarget.builder().subjectType(SubjectType.EMPLOYEE).dataKind("CARD").defaultMask(false).build();

        assertThat(t1).isEqualTo(t2);
        assertThat(t1.hashCode()).isEqualTo(t2.hashCode());
        assertThat(t1).isNotEqualTo(different);
        assertThat(t1).isNotEqualTo(null);
        assertThat(t1).isNotEqualTo("string");
        MaskingTarget nullFields = MaskingTarget.builder().build();
        assertThat(nullFields).isNotEqualTo(t1);
        assertThat(nullFields).isEqualTo(MaskingTarget.builder().build());
        assertThat(t1.toString()).contains("CUSTOMER_INDIVIDUAL");

        UnmaskAuditEvent ev1 = UnmaskAuditEvent.builder().subjectType(SubjectType.CUSTOMER_INDIVIDUAL).dataKind("RRN").fieldName("name").rowId("r1").requesterRoles(Set.of("ROLE")).build();
        UnmaskAuditEvent ev2 = UnmaskAuditEvent.builder().subjectType(SubjectType.CUSTOMER_INDIVIDUAL).dataKind("RRN").fieldName("name").rowId("r1").requesterRoles(Set.of("ROLE")).build();
        assertThat(ev1).isEqualTo(ev2);
    }

    @Test
    @DisplayName("OutputMaskingAdapter/MaskingService 강제 해제·마스킹 적용")
    void outputMaskingBranches() {
        MaskingTarget force = MaskingTarget.builder().forceUnmask(true).dataKind("REF").forceUnmaskFields(Set.of("ref"))
                .build();
        // null value
        assertThat(OutputMaskingAdapter.mask("ref", null, force, "FULL", null)).isNull();

        // force unmask returns raw
        assertThat(OutputMaskingAdapter.mask("ref", "SECRET", force, "FULL", null)).isEqualTo("SECRET");

        // forceUnmaskKinds
        MaskingTarget forceKind = MaskingTarget.builder().forceUnmaskKinds(Set.of("REF")).dataKind("REF").build();
        assertThat(OutputMaskingAdapter.mask("ref", "SECRET", forceKind, "FULL", null)).isEqualTo("SECRET");

        // forceUnmaskFields
        MaskingTarget forceField = MaskingTarget.builder().forceUnmaskFields(Set.of("ref")).dataKind("OTHER").build();
        assertThat(OutputMaskingAdapter.mask("ref", "SECRET", forceField, "FULL", null)).isEqualTo("SECRET");

        // Maskable path
        Maskable maskable = new Maskable() {
            @Override public String raw() { return "RAW-VALUE"; }
            @Override public String masked() { return "MASKED"; }
        };
        MaskingTarget normal = MaskingTarget.builder().dataKind("GENERIC").forceUnmask(false).build();
        String masked = OutputMaskingAdapter.mask("field", maskable, normal, "PARTIAL", null);
        assertThat(masked).isNotBlank();

        // MaskingService with audit sink
        java.util.List<UnmaskAuditEvent> sink = new java.util.ArrayList<>();
        MaskingStrategy strategy = (target) -> false; // 마스킹 비활성화 => 항상 raw
        MaskingService service = new MaskingService(strategy, sink::add);
        String rendered = service.render(maskable, MaskingTarget.builder().subjectType(SubjectType.CUSTOMER_INDIVIDUAL)
                .dataKind("CARD").forceUnmask(true).rowId("row1").requesterRoles(Set.of("AUDIT_ADMIN")).build(), "field");
        assertThat(rendered).isEqualTo("RAW-VALUE");
        assertThat(sink).hasSize(1);

        // 마스킹 적용시 sink 미호출
        MaskingService maskedService = new MaskingService((t) -> true, sink::add);
        assertThat(maskedService.render(maskable, normal, "field")).isEqualTo("MASKED");
    }

    @Test
    @DisplayName("RowAccessMatch/RowScopeEvaluator/Filter 브랜치")
    void policyAndRowScopeBranches() {
        RowAccessMatch match = RowAccessMatch.builder().policyId(java.util.UUID.randomUUID())
                .rowScope(RowScope.ORG).priority(1).build();
        assertThat(match).isEqualTo(RowAccessMatch.builder().policyId(match.getPolicyId())
                .rowScope(RowScope.ORG).priority(1).build());

        RowScopeContext ctx = new RowScopeContext("ORG1", List.of("ORG1", "ORG2"));
        Specification<Object> spec = RowScopeEvaluator.toSpecification(match, ctx, null);
        assertThat(spec).isNotNull();

        RowAccessMatch matchAll = RowAccessMatch.builder().rowScope(RowScope.ALL).build();
        Specification<Object> specAll = RowScopeEvaluator.toSpecification(matchAll, null, null);
        assertThat(specAll).isNotNull();

        RowScopeFilter filter = RowScopeFilter.from(match, ctx, null);
        assertThat(filter.rowScope()).isEqualTo(RowScope.ORG);
        assertThat(filter.cast()).isNotNull();
        assertThat(filter).isEqualTo(filter);
        assertThat(filter).isNotEqualTo(null);

        RowScopeFilter filterOrgAll = RowScopeFilter.from(matchAll, ctx, null);
        assertThat(filter).isNotEqualTo(filterOrgAll);

        // validation branches in RowScopeSpecifications
        assertThrows(IllegalArgumentException.class, () -> RowScopeSpecifications.organizationScoped("orgCode", RowScope.CUSTOM, "", List.of()));
        assertThrows(IllegalArgumentException.class, () -> RowScopeSpecifications.organizationScoped("orgCode", RowScope.OWN, null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> RowScopeSpecifications.organizationScoped("orgCode", RowScope.ORG, "ORG1", null));
        Specification<Object> ok = RowScopeSpecifications.organizationScoped("orgCode", RowScope.ALL, "ORG1", List.of("ORG1"));
        assertThat(ok).isNotNull();
    }
}
