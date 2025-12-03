package com.example.auth.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.auth.InvalidCredentialsException;
import com.example.auth.config.AuthPolicyProperties;
import com.example.common.user.spi.UserAccountInfo;
import com.example.common.user.spi.UserAccountProvider;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("AccountStatusPolicy 테스트")
@ExtendWith(MockitoExtension.class)
class AccountStatusPolicyTest {

  @Mock private AuthPolicyProperties properties;
  @Mock private UserAccountProvider userAccountProvider;
  @Mock private PasswordHistoryService passwordHistoryService;
  @Mock private PolicyToggleProvider policyToggleProvider;

  private AccountStatusPolicy policy;

  @BeforeEach
  void setUp() {
    policy =
        new AccountStatusPolicy(
            properties, userAccountProvider, passwordHistoryService, policyToggleProvider);
  }

  @Nested
  @DisplayName("ensureLoginAllowed 메서드")
  class EnsureLoginAllowedTests {

    @Test
    @DisplayName("Given 비활성 계정 When ensureLoginAllowed Then InvalidCredentialsException 발생")
    void givenInactiveAccount_whenEnsure_thenThrowException() {
      // Given
      UserAccountInfo account = createAccountInfo(false, false, null);

      // When & Then
      assertThatThrownBy(() -> policy.ensureLoginAllowed(account))
          .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Given 잠긴 계정 (잠금 활성화) When ensureLoginAllowed Then InvalidCredentialsException 발생")
    void givenLockedAccountWithLockEnabled_whenEnsure_thenThrowException() {
      // Given
      UserAccountInfo account = createAccountInfo(true, true, Instant.now().plusSeconds(3600));
      when(policyToggleProvider.isAccountLockEnabled()).thenReturn(true);

      // When & Then
      assertThatThrownBy(() -> policy.ensureLoginAllowed(account))
          .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Given 잠긴 계정 (잠금 비활성화) When ensureLoginAllowed Then 통과")
    void givenLockedAccountWithLockDisabled_whenEnsure_thenPass() {
      // Given
      UserAccountInfo account = createAccountInfo(true, true, Instant.now().plusSeconds(3600));
      when(policyToggleProvider.isAccountLockEnabled()).thenReturn(false);
      when(policyToggleProvider.isPasswordHistoryEnabled()).thenReturn(false);

      // When & Then - no exception
      policy.ensureLoginAllowed(account);
    }

    @Test
    @DisplayName("Given 비밀번호 만료됨 When ensureLoginAllowed Then InvalidCredentialsException 발생")
    void givenExpiredPassword_whenEnsure_thenThrowException() {
      // Given
      UserAccountInfo account = createAccountInfo(true, false, null);
      when(policyToggleProvider.isAccountLockEnabled()).thenReturn(false);
      when(policyToggleProvider.isPasswordHistoryEnabled()).thenReturn(true);
      when(passwordHistoryService.isExpired(account)).thenReturn(true);

      // When & Then
      assertThatThrownBy(() -> policy.ensureLoginAllowed(account))
          .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Given 정상 계정 When ensureLoginAllowed Then 통과")
    void givenValidAccount_whenEnsure_thenPass() {
      // Given
      UserAccountInfo account = createAccountInfo(true, false, null);
      when(policyToggleProvider.isAccountLockEnabled()).thenReturn(true);
      when(policyToggleProvider.isPasswordHistoryEnabled()).thenReturn(true);
      when(passwordHistoryService.isExpired(account)).thenReturn(false);

      // When & Then - no exception
      policy.ensureLoginAllowed(account);
    }
  }

  @Nested
  @DisplayName("onSuccessfulLogin 메서드")
  class OnSuccessfulLoginTests {

    @Test
    @DisplayName("Given 잠금 비활성화 When onSuccessfulLogin Then 아무 작업 안함")
    void givenLockDisabled_whenSuccess_thenDoNothing() {
      // Given
      UserAccountInfo account = createAccountInfo(true, false, null);
      when(policyToggleProvider.isAccountLockEnabled()).thenReturn(false);

      // When
      policy.onSuccessfulLogin(account);

      // Then
      verify(userAccountProvider, never()).resetFailedAttempts(any());
      verify(userAccountProvider, never()).lockUntil(any(), any());
    }

    @Test
    @DisplayName("Given 실패 횟수 있음 When onSuccessfulLogin Then 초기화")
    void givenFailedAttempts_whenSuccess_thenReset() {
      // Given
      UserAccountInfo account = createAccountInfoWithFailedAttempts("testuser", 3, null);
      when(policyToggleProvider.isAccountLockEnabled()).thenReturn(true);

      // When
      policy.onSuccessfulLogin(account);

      // Then
      verify(userAccountProvider).resetFailedAttempts("testuser");
      verify(userAccountProvider).lockUntil("testuser", null);
    }

    @Test
    @DisplayName("Given 잠금 시간 있음 When onSuccessfulLogin Then 잠금 해제")
    void givenLockedUntil_whenSuccess_thenUnlock() {
      // Given
      UserAccountInfo account =
          createAccountInfoWithFailedAttempts("testuser", 0, Instant.now().plusSeconds(3600));
      when(policyToggleProvider.isAccountLockEnabled()).thenReturn(true);

      // When
      policy.onSuccessfulLogin(account);

      // Then
      verify(userAccountProvider).resetFailedAttempts("testuser");
      verify(userAccountProvider).lockUntil("testuser", null);
    }

    @Test
    @DisplayName("Given 실패 횟수 0, 잠금 없음 When onSuccessfulLogin Then 아무 작업 안함")
    void givenNoFailuresNoLock_whenSuccess_thenDoNothing() {
      // Given
      UserAccountInfo account = createAccountInfoWithFailedAttempts("testuser", 0, null);
      when(policyToggleProvider.isAccountLockEnabled()).thenReturn(true);

      // When
      policy.onSuccessfulLogin(account);

      // Then
      verify(userAccountProvider, never()).resetFailedAttempts(any());
      verify(userAccountProvider, never()).lockUntil(any(), any());
    }
  }

  @Nested
  @DisplayName("onFailedLogin 메서드")
  class OnFailedLoginTests {

    @Test
    @DisplayName("Given 잠금 비활성화 When onFailedLogin Then 바로 예외 발생")
    void givenLockDisabled_whenFailed_thenThrowImmediately() {
      // Given
      UserAccountInfo account = createAccountInfo(true, false, null);
      when(policyToggleProvider.isAccountLockEnabled()).thenReturn(false);

      // When & Then
      assertThatThrownBy(() -> policy.onFailedLogin(account))
          .isInstanceOf(InvalidCredentialsException.class);
      verify(userAccountProvider, never()).incrementFailedAttempt(any());
    }

    @Test
    @DisplayName("Given 실패 횟수 초과 전 When onFailedLogin Then 실패 횟수만 증가")
    void givenUnderMaxAttempts_whenFailed_thenIncrementOnly() {
      // Given
      UserAccountInfo account = createAccountInfo(true, false, null);
      UserAccountInfo updatedAccount = createAccountInfoWithFailedAttempts("testuser", 2, null);

      when(policyToggleProvider.isAccountLockEnabled()).thenReturn(true);
      when(properties.getMaxFailedAttempts()).thenReturn(5);
      when(userAccountProvider.getByUsernameOrThrow("testuser")).thenReturn(updatedAccount);

      // When & Then
      assertThatThrownBy(() -> policy.onFailedLogin(account))
          .isInstanceOf(InvalidCredentialsException.class);
      verify(userAccountProvider).incrementFailedAttempt("testuser");
      verify(userAccountProvider, never()).lockUntil(eq("testuser"), any(Instant.class));
    }

    @Test
    @DisplayName("Given 실패 횟수 초과 When onFailedLogin Then 계정 잠금")
    void givenMaxAttemptsReached_whenFailed_thenLockAccount() {
      // Given
      UserAccountInfo account = createAccountInfo(true, false, null);
      UserAccountInfo updatedAccount = createAccountInfoWithFailedAttempts("testuser", 5, null);

      when(policyToggleProvider.isAccountLockEnabled()).thenReturn(true);
      when(properties.getMaxFailedAttempts()).thenReturn(5);
      when(properties.getLockoutSeconds()).thenReturn(900L);
      when(userAccountProvider.getByUsernameOrThrow("testuser")).thenReturn(updatedAccount);

      // When & Then
      assertThatThrownBy(() -> policy.onFailedLogin(account))
          .isInstanceOf(InvalidCredentialsException.class);
      verify(userAccountProvider).incrementFailedAttempt("testuser");
      verify(userAccountProvider).lockUntil(eq("testuser"), any(Instant.class));
      verify(userAccountProvider).resetFailedAttempts("testuser");
    }
  }

  @Nested
  @DisplayName("activate/deactivate 메서드")
  class ActivateDeactivateTests {

    @Test
    @DisplayName("Given 계정 When activate Then 활성화 호출")
    void givenAccount_whenActivate_thenCallProvider() {
      // Given
      UserAccountInfo account = createAccountInfo(false, false, null);

      // When
      policy.activate(account);

      // Then
      verify(userAccountProvider).activate("testuser");
    }

    @Test
    @DisplayName("Given 계정 When deactivate Then 비활성화 호출")
    void givenAccount_whenDeactivate_thenCallProvider() {
      // Given
      UserAccountInfo account = createAccountInfo(true, false, null);

      // When
      policy.deactivate(account);

      // Then
      verify(userAccountProvider).deactivate("testuser");
    }
  }

  private UserAccountInfo createAccountInfo(boolean active, boolean locked, Instant lockedUntil) {
    return new UserAccountInfo() {
      @Override
      public UUID getId() {
        return UUID.randomUUID();
      }

      @Override
      public String getUsername() {
        return "testuser";
      }

      @Override
      public String getPassword() {
        return "encoded";
      }

      @Override
      public String getEmail() {
        return "test@example.com";
      }

      @Override
      public Set<String> getRoles() {
        return Set.of("ROLE_USER");
      }

      @Override
      public String getOrganizationCode() {
        return "ORG001";
      }

      @Override
      public String getPermissionGroupCode() {
        return "DEFAULT";
      }

      @Override
      public String getSsoId() {
        return null;
      }

      @Override
      public String getActiveDirectoryDomain() {
        return null;
      }

      @Override
      public boolean isActive() {
        return active;
      }

      @Override
      public boolean isLocked() {
        return locked;
      }

      @Override
      public int getFailedLoginAttempts() {
        return 0;
      }

      @Override
      public Instant getLockedUntil() {
        return lockedUntil;
      }

      @Override
      public Instant getPasswordChangedAt() {
        return Instant.now();
      }
    };
  }

  private UserAccountInfo createAccountInfoWithFailedAttempts(
      String username, int failedAttempts, Instant lockedUntil) {
    return new UserAccountInfo() {
      @Override
      public UUID getId() {
        return UUID.randomUUID();
      }

      @Override
      public String getUsername() {
        return username;
      }

      @Override
      public String getPassword() {
        return "encoded";
      }

      @Override
      public String getEmail() {
        return username + "@example.com";
      }

      @Override
      public Set<String> getRoles() {
        return Set.of("ROLE_USER");
      }

      @Override
      public String getOrganizationCode() {
        return "ORG001";
      }

      @Override
      public String getPermissionGroupCode() {
        return "DEFAULT";
      }

      @Override
      public String getSsoId() {
        return null;
      }

      @Override
      public String getActiveDirectoryDomain() {
        return null;
      }

      @Override
      public boolean isActive() {
        return true;
      }

      @Override
      public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
      }

      @Override
      public int getFailedLoginAttempts() {
        return failedAttempts;
      }

      @Override
      public Instant getLockedUntil() {
        return lockedUntil;
      }

      @Override
      public Instant getPasswordChangedAt() {
        return Instant.now();
      }
    };
  }
}
