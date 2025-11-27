package com.example.admin.masking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DataPolicy 엔터티 커버리지")
class DataPolicyEntityTest {

    @Test
    void buildAndReadFields() {
        Instant now = Instant.now();
        DataPolicy policy = DataPolicy.builder()
                .featureCode("F")
                .actionCode("A")
                .permGroupCode("PG")
                .orgPolicyId(1L)
                .orgGroupCode("GRP")
                .businessType("BT")
                .rowScope("OWN")
                .rowScopeExpr("org_code = :org")
                .defaultMaskRule("PARTIAL")
                .maskParams("{\"len\":4}")
                .priority(1)
                .active(true)
                .effectiveFrom(now)
                .effectiveTo(now.plusSeconds(3600))
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertThat(policy.getFeatureCode()).isEqualTo("F");
        assertThat(policy.getRowScope()).isEqualTo("OWN");
        assertThat(policy.getDefaultMaskRule()).isEqualTo("PARTIAL");
        assertThat(policy.getEffectiveFrom()).isEqualTo(now);

        DataPolicy empty = new DataPolicy();
        assertThat(empty).isNotNull();

        DataPolicy minimal = DataPolicy.builder()
                .featureCode("F2")
                .rowScope("ALL")
                .defaultMaskRule("NONE")
                .priority(99)
                .active(false)
                .build();
        assertThat(minimal.getActive()).isFalse();
        assertThat(minimal.getPriority()).isEqualTo(99);
    }
}
