package com.example.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.auth.domain.RefreshTokenService;
import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountService;
import com.example.auth.dto.AccountStatusChangeRequest;
import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.LoginResponse;
import com.example.auth.dto.PasswordChangeRequest;
import com.example.auth.dto.TokenResponse;
import com.example.auth.security.AccountStatusPolicy;
import com.example.auth.security.JwtProperties;
import com.example.auth.security.JwtTokenProvider;
import com.example.auth.security.PasswordPolicyValidator;
import com.example.auth.security.PolicyToggleProvider;
import com.example.auth.strategy.AuthenticationStrategy;
import com.example.auth.strategy.AuthenticationStrategyResolver;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
class AuthServiceTests {

  private AuthService authService;

  @Mock private RefreshTokenService refreshTokenService;

  @Mock private UserAccountService userAccountService;

  @Mock private AccountStatusPolicy accountStatusPolicy;

  @Mock private PasswordPolicyValidator passwordPolicyValidator;

  @Mock private PolicyToggleProvider policyToggleProvider;

  @Mock private com.example.audit.AuditPort auditPort;

  @BeforeEach
  void setUp() {
    JwtProperties properties = new JwtProperties();
    properties.setSecret("test-secret-test-secret-test-secret");
    properties.setAccessTokenSeconds(3600);
    properties.setRefreshTokenSeconds(7200);
    JwtTokenProvider provider = new JwtTokenProvider(properties);
    AuthenticationStrategyResolver resolver =
        new AuthenticationStrategyResolver(List.of(new StubStrategy()));
    this.authService =
        new AuthService(
            resolver,
            provider,
            refreshTokenService,
            userAccountService,
            accountStatusPolicy,
            passwordPolicyValidator,
            policyToggleProvider,
            auditPort);
  }

  @Test
  @DisplayName("Given 비밀번호 로그인 When 인증 성공 Then 액세스·리프레시 토큰을 발급한다")
  void givenPasswordLoginWhenAuthenticatedThenIssueTokens() {
    var user =
        UserAccount.builder()
            .username("test")
            .password("pw")
            .email("test@example.com")
            .roles(Set.of("ROLE_USER"))
            .build();
    var issued =
        new RefreshTokenService.IssuedRefreshToken(
            "refresh-token", Instant.now().plusSeconds(3600), user);
    given(refreshTokenService.issue(any())).willReturn(issued);

    given(policyToggleProvider.enabledLoginTypes()).willReturn(List.of(LoginType.PASSWORD));

    var response = authService.login(new LoginRequest(LoginType.PASSWORD, "test", "pw", null));

    assertThat(response.username()).isEqualTo("test");
    assertThat(response.type()).isEqualTo(LoginType.PASSWORD);
    assertThat(response.tokens().accessToken()).isNotBlank();
    assertThat(response.tokens().refreshToken()).isEqualTo("refresh-token");
    then(refreshTokenService).should().issue(any());
  }

  @Test
  @DisplayName("Given 지원하지 않는 로그인 타입 When 로그인 요청 Then InvalidCredentialsException을 던진다")
  void givenUnsupportedTypeWhenLoginThenThrow() {
    given(policyToggleProvider.enabledLoginTypes()).willReturn(List.of(LoginType.SSO));

    assertThatThrownBy(() -> authService.login(new LoginRequest(LoginType.SSO, null, null, null)))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  @DisplayName("Given 비활성화된 로그인 타입 When 로그인 요청 Then InvalidCredentialsException을 던진다")
  void givenDisabledTypeWhenLoginThenThrow() {
    given(policyToggleProvider.enabledLoginTypes()).willReturn(List.of(LoginType.SSO));

    assertThatThrownBy(
            () -> authService.login(new LoginRequest(LoginType.PASSWORD, "user", "pw", null)))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  @DisplayName("Given 리프레시 토큰 When rotate 요청 Then 새 토큰 쌍을 반환한다")
  void givenRefreshTokenWhenRotateThenReturnNewPair() {
    var user =
        UserAccount.builder()
            .username("test")
            .password("pw")
            .email("test@example.com")
            .roles(Set.of("ROLE_USER"))
            .build();
    var rotated =
        new RefreshTokenService.IssuedRefreshToken(
            "new-refresh", Instant.now().plusSeconds(3600), user);
    given(refreshTokenService.rotate("refresh-token")).willReturn(rotated);

    TokenResponse response = authService.refreshTokens("refresh-token");

    assertThat(response.refreshToken()).isEqualTo("new-refresh");
    assertThat(response.accessToken()).isNotBlank();
    then(refreshTokenService).should().rotate("refresh-token");
  }

  @Test
  @DisplayName("Given 리프레시 토큰 When 로그아웃 호출 Then 토큰을 폐기한다")
  void givenRefreshTokenWhenLogoutThenRevoke() {
    authService.logout("refresh-token");

    then(refreshTokenService).should().revoke("refresh-token");
  }

  @Test
  @DisplayName("Given SSO 로그인 When 인증 성공 Then 토큰을 발급한다")
  void givenSsoLoginWhenAuthenticatedThenIssueTokens() {
    JwtProperties properties = new JwtProperties();
    properties.setSecret("test-secret-test-secret-test-secret");
    properties.setAccessTokenSeconds(3600);
    properties.setRefreshTokenSeconds(7200);
    JwtTokenProvider provider = new JwtTokenProvider(properties);

    AuthenticationStrategy ssoStrategy =
        new AuthenticationStrategy() {
          @Override
          public LoginType supportedType() {
            return LoginType.SSO;
          }

          @Override
          public UserAccount authenticate(LoginRequest request) {
            return UserAccount.builder()
                .username("sso-user")
                .password("pw")
                .email("sso@example.com")
                .roles(Set.of("ROLE_USER"))
                .build();
          }
        };
    AuthenticationStrategyResolver resolver =
        new AuthenticationStrategyResolver(List.of(new StubStrategy(), ssoStrategy));
    AuthService ssoAuthService =
        new AuthService(
            resolver,
            provider,
            refreshTokenService,
            userAccountService,
            accountStatusPolicy,
            passwordPolicyValidator,
            policyToggleProvider,
            auditPort);

    var issued =
        new RefreshTokenService.IssuedRefreshToken(
            "refresh-token",
            Instant.now().plusSeconds(3600),
            UserAccount.builder()
                .username("sso-user")
                .password("pw")
                .organizationCode("ORG")
                .permissionGroupCode("PG")
                .build());
    given(refreshTokenService.issue(any())).willReturn(issued);
    given(policyToggleProvider.enabledLoginTypes())
        .willReturn(List.of(LoginType.SSO, LoginType.PASSWORD));

    LoginResponse response =
        ssoAuthService.login(new LoginRequest(LoginType.SSO, null, null, "token"));

    assertThat(response.tokens().accessToken()).isNotBlank();
    assertThat(response.tokens().refreshToken()).isEqualTo("refresh-token");
  }

  @Test
  @DisplayName("Given AD 로그인 When 인증 성공 Then 토큰을 발급한다")
  void givenAdLoginWhenAuthenticatedThenIssueTokens() {
    AuthenticationStrategy adStrategy =
        new AuthenticationStrategy() {
          @Override
          public LoginType supportedType() {
            return LoginType.AD;
          }

          @Override
          public UserAccount authenticate(LoginRequest request) {
            return UserAccount.builder()
                .username("ad-user")
                .password("pw")
                .email("ad@example.com")
                .roles(Set.of("ROLE_USER"))
                .build();
          }
        };
    AuthenticationStrategyResolver resolver =
        new AuthenticationStrategyResolver(List.of(new StubStrategy(), adStrategy));
    JwtProperties properties = new JwtProperties();
    properties.setSecret("test-secret-test-secret-test-secret");
    properties.setAccessTokenSeconds(3600);
    properties.setRefreshTokenSeconds(7200);
    JwtTokenProvider provider = new JwtTokenProvider(properties);
    AuthService adAuthService =
        new AuthService(
            resolver,
            provider,
            refreshTokenService,
            userAccountService,
            accountStatusPolicy,
            passwordPolicyValidator,
            policyToggleProvider,
            auditPort);

    var issued =
        new RefreshTokenService.IssuedRefreshToken(
            "ad-refresh",
            Instant.now().plusSeconds(3600),
            UserAccount.builder()
                .username("ad-user")
                .password("pw")
                .organizationCode("ORG")
                .permissionGroupCode("PG")
                .build());
    given(refreshTokenService.issue(any())).willReturn(issued);
    given(policyToggleProvider.enabledLoginTypes())
        .willReturn(List.of(LoginType.AD, LoginType.PASSWORD));

    var response = adAuthService.login(new LoginRequest(LoginType.AD, "ad-user", "pw", null));

    assertThat(response.tokens().refreshToken()).isEqualTo("ad-refresh");
    assertThat(response.username()).isEqualTo("ad-user");
  }

  @Test
  @DisplayName("Given AD 로그인 When 인증 실패 Then InvalidCredentialsException을 던진다")
  void givenAdLoginWhenFailsThenThrows() {
    AuthenticationStrategy failingAd =
        new AuthenticationStrategy() {
          @Override
          public LoginType supportedType() {
            return LoginType.AD;
          }

          @Override
          public UserAccount authenticate(LoginRequest request) {
            throw new InvalidCredentialsException();
          }
        };
    AuthenticationStrategyResolver resolver =
        new AuthenticationStrategyResolver(List.of(new StubStrategy(), failingAd));
    JwtProperties properties = new JwtProperties();
    properties.setSecret("test-secret-test-secret-test-secret");
    properties.setAccessTokenSeconds(3600);
    properties.setRefreshTokenSeconds(7200);
    JwtTokenProvider provider = new JwtTokenProvider(properties);
    AuthService adAuthService =
        new AuthService(
            resolver,
            provider,
            refreshTokenService,
            userAccountService,
            accountStatusPolicy,
            passwordPolicyValidator,
            policyToggleProvider,
            auditPort);

    given(policyToggleProvider.enabledLoginTypes())
        .willReturn(List.of(LoginType.AD, LoginType.PASSWORD));

    assertThatThrownBy(
            () -> adAuthService.login(new LoginRequest(LoginType.AD, "user", "pw", null)))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  @DisplayName("Given 현재 비밀번호가 유효할 때 When changePassword 호출 Then 정책을 준수하며 변경한다")
  void givenValidCredentialsWhenChangePasswordThenUpdate() {
    UserAccount account =
        UserAccount.builder()
            .username("test")
            .password("encoded")
            .email("test@example.com")
            .roles(Set.of("ROLE_USER"))
            .build();
    given(userAccountService.getByUsernameOrThrow("test")).willReturn(account);
    given(userAccountService.passwordMatches(account, "current")).willReturn(true);

    authService.changePassword("test", new PasswordChangeRequest("current", "Newpassword1!"));

    then(passwordPolicyValidator).should().validate("Newpassword1!");
    then(userAccountService).should().changePassword(account, "Newpassword1!");
    then(accountStatusPolicy).should().onSuccessfulLogin(account);
  }

  @Test
  @DisplayName("Given 현재 비밀번호가 틀릴 때 When changePassword 호출 Then 실패 후 계정을 잠근다")
  void givenInvalidCurrentPasswordWhenChangePasswordThenThrow() {
    UserAccount account =
        UserAccount.builder()
            .username("test")
            .password("encoded")
            .email("test@example.com")
            .roles(Set.of("ROLE_USER"))
            .build();
    given(userAccountService.getByUsernameOrThrow("test")).willReturn(account);
    given(userAccountService.passwordMatches(account, "wrong")).willReturn(false);

    assertThatThrownBy(
            () ->
                authService.changePassword("test", new PasswordChangeRequest("wrong", "NewPass1!")))
        .isInstanceOf(InvalidCredentialsException.class);

    then(accountStatusPolicy).should().onFailedLogin(account);
    then(userAccountService).shouldHaveNoMoreInteractions();
  }

  @Test
  @DisplayName("Given status request When deactivate Then revoke tokens")
  void givenStatusRequestWhenDeactivateThenRevokeTokens() {
    UserAccount account =
        UserAccount.builder()
            .username("test")
            .password("encoded")
            .email("test@example.com")
            .roles(Set.of("ROLE_USER"))
            .build();
    given(userAccountService.getByUsernameOrThrow("test")).willReturn(account);

    authService.updateAccountStatus(new AccountStatusChangeRequest("test", false));

    then(accountStatusPolicy).should().deactivate(account);
    then(refreshTokenService).should().revokeAll(account);
  }

  private static class StubStrategy implements AuthenticationStrategy {

    @Override
    public LoginType supportedType() {
      return LoginType.PASSWORD;
    }

    @Override
    public UserAccount authenticate(LoginRequest request) {
      return UserAccount.builder()
          .username("test")
          .password("pw")
          .email("test@example.com")
          .roles(Set.of("ROLE_USER"))
          .build();
    }
  }
}
