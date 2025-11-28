package com.example.admin.policy.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.policy.PolicyToggleSettings;
import com.example.admin.policy.dto.PolicyUpdateRequest;

@DisplayName("PolicyState merge/변환 브랜치 커버리지")
class PolicyAdminServiceStateTest {

    @Test
    void mergeUpdatesFieldsAndDefaults() {
        PolicyToggleSettings defaults = new PolicyToggleSettings(true, true, true, List.of(), 10L, List.of("PDF"), true, 30,
                true, true, true, 365, true, "MEDIUM", true, List.of("/sensitive"), List.of("AUDIT_ADMIN"));
        PolicyAdminService.PolicyState state = PolicyAdminService.PolicyState.from(defaults);

        PolicyUpdateRequest req = new PolicyUpdateRequest(
                false, false, false,
                List.of("OTP"), 20L, List.of("csv"), false, 15,
                false, false, false, 90, false, "HIGH", false,
                List.of("/secure"), List.of("POWER_USER"),
                null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null);

        PolicyAdminService.PolicyState merged = state.merge(req);
        assertThat(merged.passwordPolicyEnabled()).isFalse();
        assertThat(merged.auditEnabled()).isFalse();
        assertThat(merged.auditRiskLevel()).isEqualTo("HIGH");
        assertThat(merged.auditRetentionDays()).isEqualTo(90);
        assertThat(merged.auditSensitiveEndpoints()).contains("/secure");
        assertThat(merged.auditUnmaskRoles()).contains("POWER_USER");

        // toSettings 변환 브랜치
        var settings = merged.toSettings();
        assertThat(settings.auditRiskLevel()).isEqualTo("HIGH");
        assertThat(settings.auditMaskingEnabled()).isFalse();
    }
}
