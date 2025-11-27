package com.example.common.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.common.security.RowScope;
import com.example.common.security.RowScopeContext;
import com.example.common.security.RowScopeContextHolder;
import com.example.common.security.RowScopeEvaluator;

class DataPolicySupportTest {

    @Test
    void queryNowDefaultAndContextHolderLifecycle() {
        Instant fixed = Instant.parse("2025-01-01T00:00:00Z");
        DataPolicyQuery q = new DataPolicyQuery("F", "A", "PG", 1L, List.of("ORG1"), "BT", null, fixed);
        assertThat(q.nowOrDefault()).isEqualTo(fixed);

        var match = DataPolicyMatch.builder()
                .policyId(java.util.UUID.randomUUID())
                .rowScope(RowScope.ORG.name())
                .maskRule("PARTIAL")
                .maskParams("{\"k\":\"v\"}")
                .priority(1)
                .build();

        DataPolicyContextHolder.set(match);
        assertThat(DataPolicyContextHolder.get()).isEqualTo(match);
        DataPolicyContextHolder.clear();
        assertThat(DataPolicyContextHolder.get()).isNull();
    }

    @Test
    void rowScopeEvaluatorBuildsSpecAndClears() {
        var match = DataPolicyMatch.builder().rowScope(RowScope.ORG.name()).build();
        RowScopeContext ctx = new RowScopeContext("ORG1", List.of("ORG1", "ORG1-CHILD"));
        RowScopeContextHolder.set(ctx);

        var spec = RowScopeEvaluator.toSpecification(match, null, (root, query, cb) -> cb.conjunction());
        assertThat(spec).isNotNull();

        RowScopeContextHolder.clear();
    }
}
