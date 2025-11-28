package com.example.admin.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.policy.PolicyToggleSettings;
import com.example.admin.policy.service.PolicyAdminService.PolicyState;
import com.example.admin.policy.service.PolicyAdminService.DatabasePolicySettingsProvider;

class DatabasePolicySettingsProviderTest {

    @Test
    @DisplayName("데이터베이스 설정 제공자는 PolicyAdminService의 현재 설정을 그대로 반환한다")
    void providerReturnsCurrentSettings() {
        PolicyToggleSettings toggles = new PolicyToggleSettings(true, false, true,
                java.util.List.of("PASSWORD", "SSO"), 10_000_000L,
                java.util.List.of("pdf"), true, 30);
        PolicyState state = PolicyState.from(toggles);

        PolicyAdminService service = mock(PolicyAdminService.class);
        given(service.currentState()).willReturn(state);

        DatabasePolicySettingsProvider provider = new DatabasePolicySettingsProvider(service);

        assertThat(provider.currentSettings()).usingRecursiveComparison().isEqualTo(toggles);
    }

    @Test
    @DisplayName("partitionSettings도 상태의 값을 그대로 반환한다")
    void providerReturnsPartitionSettings() {
        PolicyToggleSettings toggles = new PolicyToggleSettings(true, true, true,
                java.util.List.of("PASSWORD"), 1_000_000L, java.util.List.of("pdf"), true, 10);
        var partition = new com.example.common.policy.AuditPartitionSettings(true, "0 5 1 1 * *", 2,
                "ts_hot", "ts_cold", 12, 48);
        PolicyState state = PolicyState.from(toggles, partition, java.util.Map.of());

        PolicyAdminService service = mock(PolicyAdminService.class);
        given(service.currentState()).willReturn(state);

        DatabasePolicySettingsProvider provider = new DatabasePolicySettingsProvider(service);

        assertThat(provider.partitionSettings()).usingRecursiveComparison().isEqualTo(partition);
    }
}
