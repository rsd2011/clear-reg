package com.example.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DtoBackedPolicyToggleProviderNullTest {

  @Mock PolicySettingsProvider settingsProvider;

  @Test
  @DisplayName("설정이 null이면 기본값 false/빈 리스트가 반환된다")
  void handlesNullSettings() {
    when(settingsProvider.currentSettings())
        .thenReturn(new PolicyToggleSettings(false, false, false, null, 0L, null, false, 0));
    DtoBackedPolicyToggleProvider provider = new DtoBackedPolicyToggleProvider(settingsProvider);

    assertThat(provider.enabledLoginTypes()).isEmpty();
    assertThat(provider.isPasswordPolicyEnabled()).isFalse();
  }
}
