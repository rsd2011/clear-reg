package com.example.auth.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;

import com.example.auth.InvalidCredentialsException;
import com.example.auth.config.AuthPolicyProperties;
import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@DisplayName("AccountStatusPolicy 테스트")
class AccountStatusPolicyTest {

  @Mock private UserAccountRepository repository;

  @Mock private PasswordHistoryService passwordHistoryService;

  @Mock private PolicyToggleProvider policyToggleProvider;

  private AccountStatusPolicy policy;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    AuthPolicyProperties properties = new AuthPolicyProperties();
    properties.setMaxFailedAttempts(1);
    properties.setLockoutSeconds(60);
    org.mockito.BDDMockito.given(policyToggleProvider.isAccountLockEnabled()).willReturn(true);
    org.mockito.BDDMockito.given(policyToggleProvider.isPasswordHistoryEnabled()).willReturn(true);
    policy =
        new AccountStatusPolicy(
            properties, repository, passwordHistoryService, policyToggleProvider);
    org.mockito.BDDMockito.given(
            passwordHistoryService.isExpired(org.mockito.ArgumentMatchers.any()))
        .willReturn(false);
  }

  @Test
  @DisplayName("Given 계정이 잠금 상태일 때 When ensureLoginAllowed 호출 Then 예외를 던진다")
  void givenLockedAccountWhenEnsureThenThrow() {
    UserAccount account =
        UserAccount.builder().username("locked").password("pw").email("locked@example.com").build();
    account.lockUntil(Instant.now().plusSeconds(60));

    assertThatThrownBy(() -> policy.ensureLoginAllowed(account))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  @DisplayName("Given 비활성화 계정 When ensureLoginAllowed 호출 Then 예외를 던진다")
  void givenInactiveAccountWhenEnsureThenThrow() {
    UserAccount account =
        UserAccount.builder()
            .username("inactive")
            .password("pw")
            .email("inactive@example.com")
            .build();
    account.deactivate();

    assertThatThrownBy(() -> policy.ensureLoginAllowed(account))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  @DisplayName("Given 로그인 실패 When onFailedLogin 호출 Then 계정을 잠근다")
  void givenFailedLoginWhenOnFailedLoginThenLock() {
    UserAccount account =
        UserAccount.builder().username("user").password("pw").email("user@example.com").build();

    assertThatThrownBy(() -> policy.onFailedLogin(account))
        .isInstanceOf(InvalidCredentialsException.class);
    then(repository).should().save(account);
  }

  @Test
  @DisplayName("Given 잠금 계정 When onSuccessfulLogin 호출 Then 계정을 해제한다")
  void givenLockedAccountWhenSuccessThenUnlock() {
    UserAccount account =
        UserAccount.builder().username("user").password("pw").email("user@example.com").build();
    account.lockUntil(Instant.now().plusSeconds(60));
    account.incrementFailedAttempt();

    policy.onSuccessfulLogin(account);

    then(repository).should().save(account);
  }

  @Test
  @DisplayName("Given 비밀번호가 만료되었을 때 When ensureLoginAllowed 호출 Then 예외를 던진다")
  void givenExpiredPasswordWhenEnsureThenThrow() {
    UserAccount account =
        UserAccount.builder()
            .username("expired")
            .password("pw")
            .email("expired@example.com")
            .build();
    org.mockito.BDDMockito.given(passwordHistoryService.isExpired(account)).willReturn(true);

    assertThatThrownBy(() -> policy.ensureLoginAllowed(account))
        .isInstanceOf(InvalidCredentialsException.class);
  }
}
