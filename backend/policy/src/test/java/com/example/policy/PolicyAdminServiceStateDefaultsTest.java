package com.example.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.policy.PolicyToggleSettings;
import com.example.policy.dto.PolicyUpdateRequest;

@DisplayName("PolicyState merge 기본값/음수 분기")
class PolicyAdminServiceStateDefaultsTest {

    @Test
    void mergeWithNullsKeepsOriginalAndClampsRetention() {
        PolicyToggleSettings defaults = new PolicyToggleSettings(true, true, true, List.of("PWD"), 5L, List.of("txt"), true, 10,
                true, true, true, 30, true, "LOW", true, List.of(), List.of());
        PolicyAdminService.PolicyState state = PolicyAdminService.PolicyState.from(defaults);

        PolicyUpdateRequest req = new PolicyUpdateRequest(null, null, null,
                null, -1L, null, null, -5,
                null, null, null, -1, null, null, null, null, null,
                null, null, null, null, null);

        PolicyAdminService.PolicyState merged = state.merge(req);
        assertThat(merged.maxFileSizeBytes()).isEqualTo(5L);
        assertThat(merged.fileRetentionDays()).isEqualTo(0); // 음수 요청시 0으로 클램프
        assertThat(merged.auditRetentionDays()).isEqualTo(0); // 음수 요청시 0으로 클램프
        assertThat(merged.auditMaskingEnabled()).isTrue();
    }
}
