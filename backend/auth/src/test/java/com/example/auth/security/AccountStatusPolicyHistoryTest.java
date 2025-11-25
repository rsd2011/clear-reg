package com.example.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.auth.InvalidCredentialsException;
import com.example.auth.config.AuthPolicyProperties;
import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountStatusPolicyHistoryTest {

  private AccountStatusPolicy policy(
      boolean lockEnabled,
      boolean historyEnabled,
      PasswordHistoryService historyService,
      UserAccountRepository repo,
      AuthPolicyProperties props) {
    PolicyToggleProvider toggles = mock(PolicyToggleProvider.class);
    given(toggles.isAccountLockEnabled()).willReturn(lockEnabled);
    given(toggles.isPasswordHistoryEnabled()).willReturn(historyEnabled);
    given(toggles.isPasswordPolicyEnabled()).willReturn(true);
    given(toggles.enabledLoginTypes()).willReturn(List.of());
    given(repo.save(org.mockito.ArgumentMatchers.any())).willAnswer(inv -> inv.getArgument(0));
    return new AccountStatusPolicy(props, repo, historyService, toggles);
  }

  @Test
  @DisplayName("비잠금/비만료 계정은 ensureLoginAllowed를 통과한다")
  void ensureLoginAllowedPassesWhenUnlocked() {
    AuthPolicyProperties props = new AuthPolicyProperties();
    UserAccountRepository repo = mock(UserAccountRepository.class);
    PasswordHistoryService history = mock(PasswordHistoryService.class);
    AccountStatusPolicy policy = policy(true, false, history, repo, props);
    UserAccount user =
        UserAccount.builder()
            .username("u")
            .password("p")
            .organizationCode("ORG")
            .permissionGroupCode("PG")
            .build();

    policy.ensureLoginAllowed(user); // no exception
  }

  @Test
  @DisplayName("비밀번호 이력 만료가 true면 ensureLoginAllowed가 예외를 던진다")
  void ensureLoginAllowedFailsWhenExpired() {
    AuthPolicyProperties props = new AuthPolicyProperties();
    UserAccountRepository repo = mock(UserAccountRepository.class);
    PasswordHistoryService history = mock(PasswordHistoryService.class);
    given(history.isExpired(org.mockito.ArgumentMatchers.any())).willReturn(true);
    AccountStatusPolicy policy = policy(true, true, history, repo, props);
    UserAccount user =
        UserAccount.builder()
            .username("u")
            .password("p")
            .organizationCode("ORG")
            .permissionGroupCode("PG")
            .build();

    assertThatThrownBy(() -> policy.ensureLoginAllowed(user))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  @DisplayName("onFailedLogin 임계 도달 시 lockUntil이 설정되고 reset된다")
  void onFailedLoginLocks() {
    AuthPolicyProperties props = new AuthPolicyProperties();
    props.setMaxFailedAttempts(2);
    props.setLockoutSeconds(30);
    UserAccountRepository repo = mock(UserAccountRepository.class);
    PasswordHistoryService history = mock(PasswordHistoryService.class);
    AccountStatusPolicy policy = policy(true, false, history, repo, props);
    UserAccount user =
        UserAccount.builder()
            .username("u")
            .password("p")
            .organizationCode("ORG")
            .permissionGroupCode("PG")
            .build();

    assertThatThrownBy(() -> policy.onFailedLogin(user))
        .isInstanceOf(InvalidCredentialsException.class);
    assertThatThrownBy(() -> policy.onFailedLogin(user))
        .isInstanceOf(InvalidCredentialsException.class);

    verify(repo, org.mockito.Mockito.atLeastOnce()).save(user);
    assertThat(user.getLockedUntil()).isAfter(Instant.now());
    assertThat(user.getFailedLoginAttempts()).isZero();
  }
}
