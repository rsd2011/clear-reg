package com.example.auth.security;

import com.example.auth.LoginType;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;
import java.util.List;
import java.util.stream.Collectors;

public class DtoBackedPolicyToggleProvider implements PolicyToggleProvider {

  private final PolicySettingsProvider settingsProvider;

  public DtoBackedPolicyToggleProvider(PolicySettingsProvider settingsProvider) {
    this.settingsProvider = settingsProvider;
  }

  private PolicyToggleSettings settings() {
    return settingsProvider.currentSettings();
  }

  @Override
  public boolean isPasswordPolicyEnabled() {
    return settings().passwordPolicyEnabled();
  }

  @Override
  public boolean isPasswordHistoryEnabled() {
    return settings().passwordHistoryEnabled();
  }

  @Override
  public boolean isAccountLockEnabled() {
    return settings().accountLockEnabled();
  }

  @Override
  public List<LoginType> enabledLoginTypes() {
    return settings().enabledLoginTypes().stream()
        .map(
            name -> {
              try {
                return LoginType.valueOf(name);
              } catch (IllegalArgumentException exception) {
                return null;
              }
            })
        .filter(type -> type != null)
        .collect(Collectors.toList());
  }
}
