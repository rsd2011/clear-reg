package com.example.auth;

import com.example.audit.Actor;
import com.example.audit.ActorType;
import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPort;
import com.example.audit.RiskLevel;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final AuthenticationStrategyResolver strategyResolver;
  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenService refreshTokenService;
  private final UserAccountProvider userAccountProvider;
  private final AccountStatusPolicy accountStatusPolicy;
  private final PasswordPolicyValidator passwordPolicyValidator;
  private final PasswordHistoryService passwordHistoryService;
  private final PasswordEncoder passwordEncoder;
  private final PolicyToggleProvider policyToggleProvider;
  private final AuditPort auditPort;

  public AuthService(
      AuthenticationStrategyResolver strategyResolver,
      JwtTokenProvider jwtTokenProvider,
      RefreshTokenService refreshTokenService,
      UserAccountProvider userAccountProvider,
      AccountStatusPolicy accountStatusPolicy,
      PasswordPolicyValidator passwordPolicyValidator,
      PasswordHistoryService passwordHistoryService,
      PasswordEncoder passwordEncoder,
      PolicyToggleProvider policyToggleProvider,
      AuditPort auditPort) {
    this.strategyResolver = strategyResolver;
    this.jwtTokenProvider = jwtTokenProvider;
    this.refreshTokenService = refreshTokenService;
    this.userAccountProvider = userAccountProvider;
    this.accountStatusPolicy = accountStatusPolicy;
    this.passwordPolicyValidator = passwordPolicyValidator;
    this.passwordHistoryService = passwordHistoryService;
    this.passwordEncoder = passwordEncoder;
    this.policyToggleProvider = policyToggleProvider;
    this.auditPort = auditPort;
  }

  public LoginResponse login(LoginRequest request) {
    if (request.type() == null
        || !policyToggleProvider.enabledLoginTypes().contains(request.type())) {
      throw new InvalidCredentialsException();
    }
    AuthenticationStrategy strategy =
        strategyResolver.resolve(request.type()).orElseThrow(InvalidCredentialsException::new);
    UserAccountInfo account = strategy.authenticate(request);
    LoginResponse response =
        assembleResponse(account, request.type(), refreshTokenService.issue(account));
    recordAudit("AUTH", "LOGIN", account, true, "OK");
    return response;
  }

  public TokenResponse refreshTokens(String refreshTokenValue) {
    IssuedRefreshToken issued = refreshTokenService.rotate(refreshTokenValue);
    return buildTokenResponse(issued.user(), issued);
  }

  public void logout(String refreshTokenValue) {
    refreshTokenService.revoke(refreshTokenValue);
  }

  public void changePassword(String username, PasswordChangeRequest request) {
    UserAccountInfo account = userAccountProvider.getByUsernameOrThrow(username);
    accountStatusPolicy.ensureLoginAllowed(account);
    if (!userAccountProvider.passwordMatches(username, request.currentPassword())) {
      accountStatusPolicy.onFailedLogin(account);
      throw new InvalidCredentialsException();
    }
    passwordPolicyValidator.validate(request.newPassword());
    passwordHistoryService.ensureNotReused(username, request.newPassword());
    String encodedPassword = passwordEncoder.encode(request.newPassword());
    userAccountProvider.updatePassword(username, encodedPassword);
    passwordHistoryService.record(username, encodedPassword);
    accountStatusPolicy.onSuccessfulLogin(account);
    recordAudit("AUTH", "PASSWORD_CHANGE", account, true, "OK");
  }

  public void updateAccountStatus(AccountStatusChangeRequest request) {
    UserAccountInfo account = userAccountProvider.getByUsernameOrThrow(request.username());
    if (request.active()) {
      accountStatusPolicy.activate(account);
    } else {
      accountStatusPolicy.deactivate(account);
      refreshTokenService.revokeAll(account);
    }
  }

  private LoginResponse assembleResponse(
      UserAccountInfo account, LoginType type, IssuedRefreshToken refreshToken) {
    TokenResponse tokenResponse = buildTokenResponse(account, refreshToken);
    return new LoginResponse(account.getUsername(), type, tokenResponse);
  }

  private TokenResponse buildTokenResponse(UserAccountInfo account, IssuedRefreshToken refreshToken) {
    JwtToken accessToken =
        jwtTokenProvider.generateAccessToken(account.getUsername(), account.getRoles());
    return new TokenResponse(
        accessToken.value(),
        accessToken.expiresAt(),
        refreshToken.value(),
        refreshToken.expiresAt());
  }

  private void recordAudit(
      String eventType, String action, UserAccountInfo account, boolean success, String resultCode) {
    try {
      AuditEvent event =
          AuditEvent.builder()
              .eventType(eventType)
              .moduleName("auth")
              .action(action)
              .actor(
                  Actor.builder()
                      .id(account.getUsername())
                      .type(ActorType.HUMAN)
                      .role(account.getRoles().stream().findFirst().orElse("USER"))
                      .dept(account.getOrganizationCode())
                      .build())
              .subject(
                  com.example.audit.Subject.builder()
                      .type("USER")
                      .key(account.getUsername())
                      .build())
              .success(success)
              .resultCode(resultCode)
              .riskLevel(RiskLevel.LOW)
              .build();
      auditPort.record(event, AuditMode.ASYNC_FALLBACK);
    } catch (RuntimeException ex) {
      // dual-write: 감사 실패는 업무 흐름에 영향 없이 무시
    }
  }
}
