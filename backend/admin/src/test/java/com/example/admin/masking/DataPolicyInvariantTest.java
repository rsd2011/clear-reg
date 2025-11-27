package com.example.admin.masking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class DataPolicyInvariantTest {

    @Test
    void matchesRespectsFeatureActionPermGroupAndEffectiveWindow() {
        Instant now = Instant.parse("2025-01-01T00:00:00Z");
        DataPolicy policy = DataPolicy.builder()
                .featureCode("DRAFT")
                .actionCode("READ")
                .permGroupCode("DEFAULT")
                .rowScope("ORG")
                .defaultMaskRule("PARTIAL")
                .priority(1)
                .active(true)
                .effectiveFrom(now.minusSeconds(60))
                .effectiveTo(now.plusSeconds(60))
                .build();

        assertThat(policy.matches("DRAFT", "READ", "DEFAULT", now)).isTrue();
        assertThat(policy.matches("DRAFT", "WRITE", "DEFAULT", now)).isFalse();
        assertThat(policy.matches("DRAFT", "READ", "ADMIN", now)).isFalse();
        assertThat(policy.matches("DRAFT", null, "DEFAULT", now)).isFalse();
        assertThat(policy.matches("DRAFT", "READ", null, now)).isFalse();
        assertThat(policy.matches("DRAFT", "READ", "DEFAULT", now.minusSeconds(120))).isFalse();
        assertThat(policy.matches("DRAFT", "READ", "DEFAULT", now.plusSeconds(3600))).isFalse();
    }

    @Test
    void inactivePolicyIsNeverEffective() {
        DataPolicy policy = DataPolicy.builder()
                .featureCode("DRAFT")
                .rowScope("ALL")
                .defaultMaskRule("NONE")
                .priority(1)
                .active(false)
                .build();

        assertThat(policy.isEffectiveAt(Instant.now())).isFalse();
    }
}
