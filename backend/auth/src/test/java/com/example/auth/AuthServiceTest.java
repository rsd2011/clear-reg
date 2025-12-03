package com.example.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.audit.AuditMode;
import com.example.audit.AuditPort;
import com.example.auth.domain.RefreshTokenService;
import com.example.auth.domain.RefreshTokenService.IssuedRefreshToken;
import com.example.auth.dto.AccountStatusChangeRequest;
import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.LoginResponse;
import com.example.auth.dto.PasswordChangeRequest;
import com.example.auth.dto.TokenResponse;
import com.example.auth.security.AccountStatusPolicy;
import com.example.auth.security.JwtTokenProvider;
import com.example.auth.security.JwtTokenProvider.JwtToken;
import com.example.auth.security.PasswordHistoryService;
import com.example.auth.security.PasswordPolicyValidator;
import com.example.auth.security.PolicyToggleProvider;
import com.example.auth.strategy.AuthenticationStrategy;
import com.example.auth.strategy.AuthenticationStrategyResolver;
import com.example.common.user.spi.UserAccountInfo;
import com.example.common.user.spi.UserAccountProvider;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@DisplayName("AuthService 테스트")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private AuthenticationStrategyResolver strategyResolver;
  @Mock private JwtTokenProvider jwtTokenProvider;
  @Mock private RefreshTokenService refreshTokenService;
  @Mock private UserAccountProvider userAccountProvider;
  @Mock private AccountStatusPolicy accountStatusPolicy;
  @Mock private PasswordPolicyValidator passwordPolicyValidator;
  @Mock private PasswordHistoryService passwordHistoryService;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private PolicyToggleProvider policyToggleProvider;
  @Mock private AuditPort auditPort;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    authService =
        new AuthService(
            strategyResolver,
            jwtTokenProvider,
            refreshTokenService,
            userAccountProvider,
            accountStatusPolicy,
            passwordPolicyValidator,
            passwordHistoryService,
            passwordEncoder,
            policyToggleProvider,
            auditPort);
  }

  @Nested
  @DisplayName("login 메서드")
  class LoginTests {

    @Test
    @DisplayName("Given 유효한 로그인 요청 When login Then 토큰 응답 반환")
    void givenValidRequest_whenLogin_thenReturnTokenResponse() {
      // Given
      LoginRequest request = new LoginRequest(LoginType.PASSWORD, "testuser", "password123", null);
      UserAccountInfo account = createMockAccount("testuser");
      AuthenticationStrategy strategy = mock(AuthenticationStrategy.class);
      IssuedRefreshToken refreshToken =
          new IssuedRefreshToken("refresh-token", Instant.now().plusSeconds(3600), account);
      JwtToken accessToken =
          new JwtToken("access-token", Instant.now().plusSeconds(3600), "jti-123");

      when(policyToggleProvider.enabledLoginTypes()).thenReturn(List.of(LoginType.PASSWORD));
      when(strategyResolver.resolve(LoginType.PASSWORD)).thenReturn(Optional.of(strategy));
      when(strategy.authenticate(request)).thenReturn(account);
      when(refreshTokenService.issue(account)).thenReturn(refreshToken);
      when(jwtTokenProvider.generateAccessToken(anyString(), any())).thenReturn(accessToken);

      // When
      LoginResponse response = authService.login(request);

      // Then
      assertThat(response).isNotNull();
      assertThat(response.username()).isEqualTo("testuser");
      assertThat(response.type()).isEqualTo(LoginType.PASSWORD);
      verify(auditPort).record(any(), eq(AuditMode.ASYNC_FALLBACK));
    }

    @Test
    @DisplayName("Given null 로그인 타입 When login Then InvalidCredentialsException 발생")
    void givenNullType_whenLogin_thenThrowException() {
      // Given
      LoginRequest request = new LoginRequest(null, "testuser", "password123", null);

      // When & Then
      assertThatThrownBy(() -> authService.login(request))
          .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Given 비활성화된 로그인 타입 When login Then InvalidCredentialsException 발생")
    void givenDisabledType_whenLogin_thenThrowException() {
      // Given
      LoginRequest request = new LoginRequest(LoginType.SSO, "testuser", null, "token");
      when(policyToggleProvider.enabledLoginTypes()).thenReturn(List.of(LoginType.PASSWORD));

      // When & Then
      assertThatThrownBy(() -> authService.login(request))
          .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Given 전략을 찾을 수 없음 When login Then InvalidCredentialsException 발생")
    void givenNoStrategy_whenLogin_thenThrowException() {
      // Given
      LoginRequest request = new LoginRequest(LoginType.PASSWORD, "testuser", "password123", null);
      when(policyToggleProvider.enabledLoginTypes()).thenReturn(List.of(LoginType.PASSWORD));
      when(strategyResolver.resolve(LoginType.PASSWORD)).thenReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> authService.login(request))
          .isInstanceOf(InvalidCredentialsException.class);
    }
  }

  @Nested
  @DisplayName("refreshTokens 메서드")
  class RefreshTokensTests {

    @Test
    @DisplayName("Given 유효한 리프레시 토큰 When refreshTokens Then 새 토큰 반환")
    void givenValidRefreshToken_whenRefresh_thenReturnNewTokens() {
      // Given
      String refreshTokenValue = "valid-refresh-token";
      UserAccountInfo account = createMockAccount("testuser");
      IssuedRefreshToken newRefreshToken =
          new IssuedRefreshToken("new-refresh-token", Instant.now().plusSeconds(3600), account);
      JwtToken accessToken =
          new JwtToken("new-access-token", Instant.now().plusSeconds(3600), "jti-456");

      when(refreshTokenService.rotate(refreshTokenValue)).thenReturn(newRefreshToken);
      when(jwtTokenProvider.generateAccessToken(anyString(), any())).thenReturn(accessToken);

      // When
      TokenResponse response = authService.refreshTokens(refreshTokenValue);

      // Then
      assertThat(response).isNotNull();
      assertThat(response.accessToken()).isEqualTo("new-access-token");
      assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
    }
  }

  @Nested
  @DisplayName("logout 메서드")
  class LogoutTests {

    @Test
    @DisplayName("Given 리프레시 토큰 When logout Then 토큰 폐기")
    void givenRefreshToken_whenLogout_thenRevokeToken() {
      // Given
      String refreshTokenValue = "refresh-token";

      // When
      authService.logout(refreshTokenValue);

      // Then
      verify(refreshTokenService).revoke(refreshTokenValue);
    }
  }

  @Nested
  @DisplayName("changePassword 메서드")
  class ChangePasswordTests {

    @Test
    @DisplayName("Given 유효한 비밀번호 변경 요청 When changePassword Then 성공")
    void givenValidRequest_whenChangePassword_thenSuccess() {
      // Given
      String username = "testuser";
      PasswordChangeRequest request = new PasswordChangeRequest("oldPassword", "newPassword123!");
      UserAccountInfo account = createMockAccount(username);
      String encodedPassword = "encoded-new-password";

      when(userAccountProvider.getByUsernameOrThrow(username)).thenReturn(account);
      doNothing().when(accountStatusPolicy).ensureLoginAllowed(account);
      when(userAccountProvider.passwordMatches(username, "oldPassword")).thenReturn(true);
      doNothing().when(passwordPolicyValidator).validate("newPassword123!");
      doNothing().when(passwordHistoryService).ensureNotReused(username, "newPassword123!");
      when(passwordEncoder.encode("newPassword123!")).thenReturn(encodedPassword);

      // When
      authService.changePassword(username, request);

      // Then
      verify(userAccountProvider).updatePassword(username, encodedPassword);
      verify(passwordHistoryService).record(username, encodedPassword);
      verify(accountStatusPolicy).onSuccessfulLogin(account);
      verify(auditPort).record(any(), eq(AuditMode.ASYNC_FALLBACK));
    }

    @Test
    @DisplayName("Given 잘못된 현재 비밀번호 When changePassword Then InvalidCredentialsException 발생")
    void givenWrongCurrentPassword_whenChangePassword_thenThrowException() {
      // Given
      String username = "testuser";
      PasswordChangeRequest request = new PasswordChangeRequest("wrongPassword", "newPassword123!");
      UserAccountInfo account = createMockAccount(username);

      when(userAccountProvider.getByUsernameOrThrow(username)).thenReturn(account);
      doNothing().when(accountStatusPolicy).ensureLoginAllowed(account);
      when(userAccountProvider.passwordMatches(username, "wrongPassword")).thenReturn(false);
      doThrow(new InvalidCredentialsException())
          .when(accountStatusPolicy)
          .onFailedLogin(account);

      // When & Then
      assertThatThrownBy(() -> authService.changePassword(username, request))
          .isInstanceOf(InvalidCredentialsException.class);
    }
  }

  @Nested
  @DisplayName("updateAccountStatus 메서드")
  class UpdateAccountStatusTests {

    @Test
    @DisplayName("Given 활성화 요청 When updateAccountStatus Then 계정 활성화")
    void givenActivateRequest_whenUpdate_thenActivateAccount() {
      // Given
      AccountStatusChangeRequest request = new AccountStatusChangeRequest("testuser", true);
      UserAccountInfo account = createMockAccount("testuser");

      when(userAccountProvider.getByUsernameOrThrow("testuser")).thenReturn(account);

      // When
      authService.updateAccountStatus(request);

      // Then
      verify(accountStatusPolicy).activate(account);
      verify(refreshTokenService, never()).revokeAll(any());
    }

    @Test
    @DisplayName("Given 비활성화 요청 When updateAccountStatus Then 계정 비활성화 및 토큰 폐기")
    void givenDeactivateRequest_whenUpdate_thenDeactivateAndRevokeTokens() {
      // Given
      AccountStatusChangeRequest request = new AccountStatusChangeRequest("testuser", false);
      UserAccountInfo account = createMockAccount("testuser");

      when(userAccountProvider.getByUsernameOrThrow("testuser")).thenReturn(account);

      // When
      authService.updateAccountStatus(request);

      // Then
      verify(accountStatusPolicy).deactivate(account);
      verify(refreshTokenService).revokeAll(account);
    }
  }

  private UserAccountInfo createMockAccount(String username) {
    UserAccountInfo account = mock(UserAccountInfo.class);
    lenient().when(account.getUsername()).thenReturn(username);
    lenient().when(account.getRoles()).thenReturn(Set.of("ROLE_USER"));
    lenient().when(account.getOrganizationCode()).thenReturn("ORG001");
    return account;
  }
}
