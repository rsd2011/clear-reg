package com.example.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuditPolicySnapshotBranchTest {

    @Test
    @DisplayName("secureDefault는 기본 플래그를 설정하고 equals/hashCode가 동작한다")
    void secureDefaultAndEquals() {
        AuditPolicySnapshot defaultSnap = AuditPolicySnapshot.secureDefault();
        AuditPolicySnapshot same = AuditPolicySnapshot.builder().build();

        assertThat(defaultSnap).isEqualTo(same);
        assertThat(defaultSnap.hashCode()).isEqualTo(same.hashCode());
        assertThat(defaultSnap.isEnabled()).isTrue();
        assertThat(defaultSnap.isReasonRequired()).isTrue();
    }

    @Test
    @DisplayName("마스킹/리스크/속성 차이가 있으면 equals는 false")
    void equalsFalseWhenAttributesDiffer() {
        AuditPolicySnapshot base = AuditPolicySnapshot.builder()
                .maskingEnabled(true)
                .riskLevel(RiskLevel.MEDIUM)
                .retentionDays(365)
                .attribute("k", "v")
                .build();
        AuditPolicySnapshot differentMask = base.toBuilder().maskingEnabled(false).build();
        AuditPolicySnapshot differentAttr = base.toBuilder().attributes(Map.of("k", "other")).build();

        assertThat(base).isNotEqualTo(differentMask);
        assertThat(base).isNotEqualTo(differentAttr);
    }
}
