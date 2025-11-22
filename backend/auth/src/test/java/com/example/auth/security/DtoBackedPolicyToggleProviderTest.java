package com.example.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.auth.LoginType;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;

@ExtendWith(MockitoExtension.class)
class DtoBackedPolicyToggleProviderTest {

    @Mock
    PolicySettingsProvider settingsProvider;

    @Test
    @DisplayName("enabledLoginTypes에 잘못된 값이 있어도 null을 제거하고 변환한다")
    void enabledLoginTypesFiltersInvalid() {
        PolicyToggleSettings settings = new PolicyToggleSettings(true, true, true, List.of("PASSWORD", "INVALID"), 0L, List.of(), false, 30);
        when(settingsProvider.currentSettings()).thenReturn(settings);

        DtoBackedPolicyToggleProvider provider = new DtoBackedPolicyToggleProvider(settingsProvider);

        assertThat(provider.enabledLoginTypes()).containsExactly(LoginType.PASSWORD);
    }
}
