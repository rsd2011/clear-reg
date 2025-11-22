package com.example.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuditPolicySnapshotTest {

    @Test
    void secureDefault_isSecureByDefault() {
        var snapshot = AuditPolicySnapshot.secureDefault();

        assertThat(snapshot.isEnabled()).isTrue();
        assertThat(snapshot.isReasonRequired()).isTrue();
        assertThat(snapshot.isSensitiveApi()).isFalse();
        assertThat(snapshot.getMode()).isEqualTo(AuditMode.STRICT);
        assertThat(snapshot.getRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
    }

    @Test
    void builder_overridesFields() {
        var snapshot = AuditPolicySnapshot.builder()
                .enabled(false)
                .sensitiveApi(true)
                .reasonRequired(false)
                .mode(AuditMode.ASYNC_FALLBACK)
                .retentionDays(365)
                .riskLevel(RiskLevel.HIGH)
                .attribute("key", "value")
                .build();

        assertThat(snapshot.isEnabled()).isFalse();
        assertThat(snapshot.isSensitiveApi()).isTrue();
        assertThat(snapshot.isReasonRequired()).isFalse();
        assertThat(snapshot.getMode()).isEqualTo(AuditMode.ASYNC_FALLBACK);
        assertThat(snapshot.getRetentionDays()).isEqualTo(365);
        assertThat(snapshot.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(snapshot.getAttributes()).containsEntry("key", "value");
    }
}
