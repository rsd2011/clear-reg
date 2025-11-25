package com.example.auth.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.auth.InvalidCredentialsException;
import com.example.auth.config.AuthPolicyProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordPolicyValidatorTest {

  private final AuthPolicyProperties props = new AuthPolicyProperties();

  private static class StubToggleProvider implements PolicyToggleProvider {
    private final boolean enabled;

    StubToggleProvider(boolean enabled) {
      this.enabled = enabled;
    }

    @Override
    public boolean isPasswordPolicyEnabled() {
      return enabled;
    }

    @Override
    public boolean isPasswordHistoryEnabled() {
      return false;
    }

    @Override
    public boolean isAccountLockEnabled() {
      return false;
    }

    @Override
    public java.util.List<com.example.auth.LoginType> enabledLoginTypes() {
      return java.util.List.of();
    }
  }

  @DisplayName("토글이 꺼져 있으면 비밀번호 검증을 건너뛴다")
  @Test
  void validate_skipsWhenDisabled() {
    PasswordPolicyValidator validator =
        new PasswordPolicyValidator(props, new StubToggleProvider(false));

    assertThatCode(() -> validator.validate(null)).doesNotThrowAnyException();
  }

  @DisplayName("숫자를 요구하지만 숫자가 없으면 InvalidCredentialsException을 던진다")
  @Test
  void validate_requiresDigit() {
    props.setPasswordMinLength(4);
    props.setRequireDigit(true);
    PasswordPolicyValidator validator =
        new PasswordPolicyValidator(props, new StubToggleProvider(true));

    assertThatThrownBy(() -> validator.validate("Abc!"))
        .isInstanceOf(InvalidCredentialsException.class);
  }
}
