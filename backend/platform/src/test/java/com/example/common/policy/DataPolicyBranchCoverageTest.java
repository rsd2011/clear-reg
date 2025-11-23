package com.example.common.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("데이터 정책 DTO 브랜치 커버리지")
class DataPolicyBranchCoverageTest {

    @Test
    @DisplayName("DataPolicyMatch equals/hashCode 다양한 필드")
    void dataPolicyMatchBranches() {
        UUID id = UUID.randomUUID();
        DataPolicyMatch match1 = DataPolicyMatch.builder().policyId(id).rowScope("ORG").rowScopeExpr("org_code")
                .maskRule("FULL").maskParams("{}").priority(10).build();
        DataPolicyMatch match2 = DataPolicyMatch.builder().policyId(id).rowScope("ORG").rowScopeExpr("org_code")
                .maskRule("FULL").maskParams("{}").priority(10).build();
        DataPolicyMatch match3 = DataPolicyMatch.builder().policyId(UUID.randomUUID()).rowScope("ALL").build();

        assertThat(match1).isEqualTo(match2);
        assertThat(match1).isNotEqualTo(match3);
        assertThat(match1.toString()).contains("maskRule");
        assertThat(match1).isEqualTo(match1);
        assertThat(match1).isNotEqualTo(null);
        assertThat(match1).isNotEqualTo("string");

        // builder 기본값
        DataPolicyMatch empty = DataPolicyMatch.builder().build();
        assertThat(empty.getMaskRule()).isNull();
        assertThat(empty).isNotEqualTo(match1);
        assertThat(empty).isEqualTo(DataPolicyMatch.builder().build());

        assertThat(match1).isNotEqualTo(builderFrom(match1).policyId(UUID.randomUUID()).build());
        assertThat(match1).isNotEqualTo(builderFrom(match1).rowScope("ALL").build());
        assertThat(match1).isNotEqualTo(builderFrom(match1).maskRule("PARTIAL").build());
        assertThat(match1).isNotEqualTo(builderFrom(match1).priority(99).build());

        // hashCode 브랜치
        match1.hashCode();
        empty.hashCode();
    }

    private DataPolicyMatch.DataPolicyMatchBuilder builderFrom(DataPolicyMatch base) {
        return DataPolicyMatch.builder()
                .policyId(base.getPolicyId())
                .rowScope(base.getRowScope())
                .rowScopeExpr(base.getRowScopeExpr())
                .maskRule(base.getMaskRule())
                .maskParams(base.getMaskParams())
                .priority(base.getPriority());
    }

    @Test
    @DisplayName("DataPolicyQuery now 기본값")
    void dataPolicyQueryNowDefault() {
        DataPolicyQuery queryWithNow = new DataPolicyQuery("F", "A", "PG", 1L, List.of("G1"), "BT", Instant.EPOCH);
        assertThat(queryWithNow.nowOrDefault()).isEqualTo(Instant.EPOCH);

        DataPolicyQuery queryNoNow = new DataPolicyQuery("F", "A", "PG", 1L, List.of("G1"), "BT", null);
        assertThat(queryNoNow.nowOrDefault()).isNotNull();
    }
}
