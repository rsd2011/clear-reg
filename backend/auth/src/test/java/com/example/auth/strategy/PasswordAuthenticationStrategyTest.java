package com.example.auth.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.auth.InvalidCredentialsException;
import com.example.auth.LoginType;
import com.example.auth.dto.LoginRequest;
import com.example.auth.security.AccountStatusPolicy;
import com.example.auth.security.PasswordPolicyValidator;
import com.example.common.user.spi.UserAccountInfo;
import com.example.common.user.spi.UserAccountProvider;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("PasswordAuthenticationStrategy 테스트")
@ExtendWith(MockitoExtension.class)
class PasswordAuthenticationStrategyTest {

  @Mock private UserAccountProvider userAccountProvider;
  @Mock private PasswordPolicyValidator passwordPolicyValidator;
  @Mock private AccountStatusPolicy accountStatusPolicy;

  private PasswordAuthenticationStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy =
        new PasswordAuthenticationStrategy(
            userAccountProvider, passwordPolicyValidator, accountStatusPolicy);
  }

  @Test
  @DisplayName("supportedType은 PASSWORD를 반환해야 함")
  void supportedType_returnsPassword() {
    assertThat(strategy.supportedType()).isEqualTo(LoginType.PASSWORD);
  }

  @Test
  @DisplayName("Given null username When authenticate Then InvalidCredentialsException 발생")
  void givenNullUsername_whenAuthenticate_thenThrowException() {
    // Given
    LoginRequest request = new LoginRequest(LoginType.PASSWORD, null, "password", null);

    // When & Then
    assertThatThrownBy(() -> strategy.authenticate(request))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  @DisplayName("Given null password When authenticate Then InvalidCredentialsException 발생")
  void givenNullPassword_whenAuthenticate_thenThrowException() {
    // Given
    LoginRequest request = new LoginRequest(LoginType.PASSWORD, "testuser", null, null);

    // When & Then
    assertThatThrownBy(() -> strategy.authenticate(request))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  @DisplayName("Given 유효한 자격증명 When authenticate Then 계정 반환")
  void givenValidCredentials_whenAuthenticate_thenReturnAccount() {
    // Given
    LoginRequest request = new LoginRequest(LoginType.PASSWORD, "testuser", "ValidPass123!", null);
    UserAccountInfo account = createAccountInfo("testuser");

    doNothing().when(passwordPolicyValidator).validate("ValidPass123!");
    when(userAccountProvider.getByUsernameOrThrow("testuser")).thenReturn(account);
    doNothing().when(accountStatusPolicy).ensureLoginAllowed(account);
    when(userAccountProvider.passwordMatches("testuser", "ValidPass123!")).thenReturn(true);
    doNothing().when(accountStatusPolicy).onSuccessfulLogin(account);

    // When
    UserAccountInfo result = strategy.authenticate(request);

    // Then
    assertThat(result).isEqualTo(account);
    verify(accountStatusPolicy).onSuccessfulLogin(account);
  }

  @Test
  @DisplayName("Given 잘못된 비밀번호 When authenticate Then onFailedLogin 호출")
  void givenWrongPassword_whenAuthenticate_thenCallOnFailedLogin() {
    // Given
    LoginRequest request = new LoginRequest(LoginType.PASSWORD, "testuser", "WrongPass123!", null);
    UserAccountInfo account = createAccountInfo("testuser");

    doNothing().when(passwordPolicyValidator).validate("WrongPass123!");
    when(userAccountProvider.getByUsernameOrThrow("testuser")).thenReturn(account);
    doNothing().when(accountStatusPolicy).ensureLoginAllowed(account);
    when(userAccountProvider.passwordMatches("testuser", "WrongPass123!")).thenReturn(false);
    doThrow(new InvalidCredentialsException())
        .when(accountStatusPolicy)
        .onFailedLogin(account);

    // When & Then
    assertThatThrownBy(() -> strategy.authenticate(request))
        .isInstanceOf(InvalidCredentialsException.class);
    verify(accountStatusPolicy).onFailedLogin(account);
  }

  @Test
  @DisplayName("Given 비활성 계정 When authenticate Then InvalidCredentialsException 발생")
  void givenInactiveAccount_whenAuthenticate_thenThrowException() {
    // Given
    LoginRequest request = new LoginRequest(LoginType.PASSWORD, "testuser", "ValidPass123!", null);
    UserAccountInfo account = createAccountInfo("testuser");

    doNothing().when(passwordPolicyValidator).validate("ValidPass123!");
    when(userAccountProvider.getByUsernameOrThrow("testuser")).thenReturn(account);
    doThrow(new InvalidCredentialsException())
        .when(accountStatusPolicy)
        .ensureLoginAllowed(account);

    // When & Then
    assertThatThrownBy(() -> strategy.authenticate(request))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  private UserAccountInfo createAccountInfo(String username) {
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
        return false;
      }

      @Override
      public int getFailedLoginAttempts() {
        return 0;
      }

      @Override
      public Instant getLockedUntil() {
        return null;
      }

      @Override
      public Instant getPasswordChangedAt() {
        return Instant.now();
      }
    };
  }
}
