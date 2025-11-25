package com.example.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.example.auth.InvalidCredentialsException;
import com.example.auth.config.AuthPolicyProperties;
import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountRepository;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountStatusPolicyBranchTest {

  private AccountStatusPolicy policy(AuthPolicyProperties props) {
    UserAccountRepository repo = mock(UserAccountRepository.class);
    PasswordHistoryService history = mock(PasswordHistoryService.class);
    PolicyToggleProvider toggles = mock(PolicyToggleProvider.class);
    given(toggles.isAccountLockEnabled()).willReturn(true);
    given(toggles.isPasswordHistoryEnabled()).willReturn(false);
    given(toggles.isPasswordPolicyEnabled()).willReturn(true);
    given(toggles.enabledLoginTypes()).willReturn(java.util.List.of());
    given(repo.save(org.mockito.ArgumentMatchers.any())).willAnswer(inv -> inv.getArgument(0));
    return new AccountStatusPolicy(props, repo, history, toggles);
  }

  @Test
  @DisplayName("3회 실패 시 잠기고 ensureLoginAllowed가 예외를 던진다")
  void locksAfterThreeFailures() {
    AuthPolicyProperties props = new AuthPolicyProperties();
    props.setMaxFailedAttempts(3);
    props.setLockoutSeconds(60);
    AccountStatusPolicy policy = policy(props);
    UserAccount user =
        UserAccount.builder()
            .username("u")
            .password("p")
            .organizationCode("ORG")
            .permissionGroupCode("PG")
            .build();

    assertThrows(InvalidCredentialsException.class, () -> policy.onFailedLogin(user));
    assertThrows(InvalidCredentialsException.class, () -> policy.onFailedLogin(user));
    assertThrows(InvalidCredentialsException.class, () -> policy.onFailedLogin(user));

    assertThat(user.getLockedUntil()).isAfter(Instant.now());
    assertThrows(InvalidCredentialsException.class, () -> policy.ensureLoginAllowed(user));
  }

  @Test
  @DisplayName("잠금 상태에서 onSuccessfulLogin은 잠금을 해제하고 실패 횟수를 리셋한다")
  void unlocksOnSuccess() {
    AuthPolicyProperties props = new AuthPolicyProperties();
    props.setMaxFailedAttempts(1);
    props.setLockoutSeconds(60);
    AccountStatusPolicy policy = policy(props);
    UserAccount user =
        UserAccount.builder()
            .username("u")
            .password("p")
            .organizationCode("ORG")
            .permissionGroupCode("PG")
            .build();

    assertThrows(InvalidCredentialsException.class, () -> policy.onFailedLogin(user));
    assertThat(user.isLocked()).isTrue();

    policy.onSuccessfulLogin(user);

    assertThat(user.isLocked()).isFalse();
    assertThat(user.getFailedLoginAttempts()).isZero();
  }

  @Test
  @DisplayName("락 토글이 꺼져 있으면 잠금 상태라도 로그인 허용한다")
  void lockToggleOffAllowsLogin() {
    AuthPolicyProperties props = new AuthPolicyProperties();
    props.setMaxFailedAttempts(1);
    props.setLockoutSeconds(60);

    UserAccountRepository repo = mock(UserAccountRepository.class);
    PasswordHistoryService history = mock(PasswordHistoryService.class);
    PolicyToggleProvider toggles = mock(PolicyToggleProvider.class);
    given(toggles.isAccountLockEnabled()).willReturn(false);
    given(toggles.isPasswordHistoryEnabled()).willReturn(false);
    given(toggles.isPasswordPolicyEnabled()).willReturn(true);
    given(toggles.enabledLoginTypes()).willReturn(java.util.List.of());

    AccountStatusPolicy policy = new AccountStatusPolicy(props, repo, history, toggles);
    UserAccount locked =
        UserAccount.builder()
            .username("u")
            .password("p")
            .organizationCode("ORG")
            .permissionGroupCode("PG")
            .build();
    locked.lockUntil(Instant.now().plusSeconds(120));

    // should not throw because lock toggle is off
    policy.ensureLoginAllowed(locked);
  }

  @Test
  @DisplayName("비밀번호 이력 토글이 켜져 있고 만료이면 ensureLoginAllowed가 예외를 던진다")
  void passwordHistoryEnabledAndExpiredThrows() {
    AuthPolicyProperties props = new AuthPolicyProperties();
    UserAccountRepository repo = mock(UserAccountRepository.class);
    PasswordHistoryService history = mock(PasswordHistoryService.class);
    PolicyToggleProvider toggles = mock(PolicyToggleProvider.class);
    given(toggles.isAccountLockEnabled()).willReturn(true);
    given(toggles.isPasswordHistoryEnabled()).willReturn(true);
    given(toggles.isPasswordPolicyEnabled()).willReturn(true);
    given(toggles.enabledLoginTypes()).willReturn(java.util.List.of());
    given(history.isExpired(org.mockito.ArgumentMatchers.any())).willReturn(true);

    AccountStatusPolicy policy = new AccountStatusPolicy(props, repo, history, toggles);
    UserAccount account =
        UserAccount.builder()
            .username("u")
            .password("p")
            .organizationCode("ORG")
            .permissionGroupCode("PG")
            .build();

    assertThrows(InvalidCredentialsException.class, () -> policy.ensureLoginAllowed(account));
  }

  @Test
  @DisplayName("비밀번호 이력 토글이 켜져 있고 만료가 아니면 ensureLoginAllowed를 통과한다")
  void passwordHistoryEnabledAndNotExpiredPasses() {
    AuthPolicyProperties props = new AuthPolicyProperties();
    UserAccountRepository repo = mock(UserAccountRepository.class);
    PasswordHistoryService history = mock(PasswordHistoryService.class);
    PolicyToggleProvider toggles = mock(PolicyToggleProvider.class);
    given(toggles.isAccountLockEnabled()).willReturn(true);
    given(toggles.isPasswordHistoryEnabled()).willReturn(true);
    given(toggles.isPasswordPolicyEnabled()).willReturn(true);
    given(toggles.enabledLoginTypes()).willReturn(java.util.List.of());
    given(history.isExpired(org.mockito.ArgumentMatchers.any())).willReturn(false);

    AccountStatusPolicy policy = new AccountStatusPolicy(props, repo, history, toggles);
    UserAccount account =
        UserAccount.builder()
            .username("u")
            .password("p")
            .organizationCode("ORG")
            .permissionGroupCode("PG")
            .build();

    policy.ensureLoginAllowed(account);
  }

  @Test
  @DisplayName("락 OFF + 히스토리 ON 이지만 만료되지 않으면 로그인 허용한다")
  void lockOffHistoryOnNotExpiredPasses() {
    AuthPolicyProperties props = new AuthPolicyProperties();
    UserAccountRepository repo = mock(UserAccountRepository.class);
    PasswordHistoryService history = mock(PasswordHistoryService.class);
    PolicyToggleProvider toggles = mock(PolicyToggleProvider.class);
    given(toggles.isAccountLockEnabled()).willReturn(false);
    given(toggles.isPasswordHistoryEnabled()).willReturn(true);
    given(toggles.isPasswordPolicyEnabled()).willReturn(true);
    given(toggles.enabledLoginTypes()).willReturn(java.util.List.of());
    given(history.isExpired(org.mockito.ArgumentMatchers.any())).willReturn(false);

    AccountStatusPolicy policy = new AccountStatusPolicy(props, repo, history, toggles);
    UserAccount account =
        UserAccount.builder()
            .username("u")
            .password("p")
            .organizationCode("ORG")
            .permissionGroupCode("PG")
            .build();

    policy.ensureLoginAllowed(account);
  }

  @Test
  @DisplayName("락 ON이고 계정이 locked이면 즉시 로그인 거부한다")
  void lockOnAndLockedThrows() {
    AuthPolicyProperties props = new AuthPolicyProperties();
    UserAccountRepository repo = mock(UserAccountRepository.class);
    PasswordHistoryService history = mock(PasswordHistoryService.class);
    PolicyToggleProvider toggles = mock(PolicyToggleProvider.class);
    given(toggles.isAccountLockEnabled()).willReturn(true);
    given(toggles.isPasswordHistoryEnabled()).willReturn(false);
    given(toggles.isPasswordPolicyEnabled()).willReturn(true);
    given(toggles.enabledLoginTypes()).willReturn(java.util.List.of());

    AccountStatusPolicy policy = new AccountStatusPolicy(props, repo, history, toggles);
    UserAccount locked =
        UserAccount.builder()
            .username("locked")
            .password("pw")
            .organizationCode("ORG")
            .permissionGroupCode("PG")
            .build();
    locked.lockUntil(Instant.now().plusSeconds(120));

    assertThrows(InvalidCredentialsException.class, () -> policy.ensureLoginAllowed(locked));
  }

  @Test
  @DisplayName("계정이 비활성화 상태면 ensureLoginAllowed가 예외를 던진다")
  void inactiveAccountThrows() {
    AuthPolicyProperties props = new AuthPolicyProperties();
    UserAccountRepository repo = mock(UserAccountRepository.class);
    PasswordHistoryService history = mock(PasswordHistoryService.class);
    PolicyToggleProvider toggles = mock(PolicyToggleProvider.class);
    given(toggles.isAccountLockEnabled()).willReturn(true);
    given(toggles.isPasswordHistoryEnabled()).willReturn(false);
    given(toggles.isPasswordPolicyEnabled()).willReturn(true);
    given(toggles.enabledLoginTypes()).willReturn(java.util.List.of());

    AccountStatusPolicy policy = new AccountStatusPolicy(props, repo, history, toggles);
    UserAccount inactive =
        UserAccount.builder()
            .username("inactive")
            .password("pw")
            .organizationCode("ORG")
            .permissionGroupCode("PG")
            .build();
    inactive.deactivate();

    assertThrows(InvalidCredentialsException.class, () -> policy.ensureLoginAllowed(inactive));
  }

  @Test
  @DisplayName("락 OFF여도 히스토리 만료면 로그인 거부한다")
  void lockOffHistoryExpiredThrows() {
    AuthPolicyProperties props = new AuthPolicyProperties();
    UserAccountRepository repo = mock(UserAccountRepository.class);
    PasswordHistoryService history = mock(PasswordHistoryService.class);
    PolicyToggleProvider toggles = mock(PolicyToggleProvider.class);
    given(toggles.isAccountLockEnabled()).willReturn(false);
    given(toggles.isPasswordHistoryEnabled()).willReturn(true);
    given(toggles.isPasswordPolicyEnabled()).willReturn(true);
    given(toggles.enabledLoginTypes()).willReturn(java.util.List.of());
    given(history.isExpired(org.mockito.ArgumentMatchers.any())).willReturn(true);

    AccountStatusPolicy policy = new AccountStatusPolicy(props, repo, history, toggles);
    UserAccount user =
        UserAccount.builder()
            .username("u")
            .password("p")
            .organizationCode("ORG")
            .permissionGroupCode("PG")
            .build();

    assertThrows(InvalidCredentialsException.class, () -> policy.ensureLoginAllowed(user));
  }
}
