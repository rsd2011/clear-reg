package com.example.auth.domain;

import com.example.admin.user.domain.UserAccount;
import com.example.admin.user.repository.UserAccountRepository;
import com.example.auth.InvalidCredentialsException;
import com.example.auth.config.SessionPolicyProperties;
import com.example.auth.security.JwtProperties;
import com.example.common.user.spi.UserAccountInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Service and nested record return domain references intentionally")
@Service
public class RefreshTokenService {

  private final RefreshTokenRepository repository;
  private final UserAccountRepository userAccountRepository;
  private final JwtProperties properties;
  private final SessionPolicyProperties sessionPolicyProperties;
  private final SecureRandom secureRandom = new SecureRandom();

  public RefreshTokenService(
      RefreshTokenRepository repository,
      UserAccountRepository userAccountRepository,
      JwtProperties properties,
      SessionPolicyProperties sessionPolicyProperties) {
    this.repository = repository;
    this.userAccountRepository = userAccountRepository;
    this.properties = properties;
    this.sessionPolicyProperties = sessionPolicyProperties;
  }

  @Transactional
  public IssuedRefreshToken issue(UserAccountInfo accountInfo) {
    return issue(accountInfo, false);
  }

  @Transactional
  public IssuedRefreshToken issue(UserAccountInfo accountInfo, boolean rememberMe) {
    UserAccount userAccount = resolveUserAccount(accountInfo);
    String rawToken = generateTokenValue();
    long lifetimeSeconds =
        rememberMe
            ? properties.getRememberMeRefreshTokenSeconds()
            : properties.getDefaultRefreshTokenSeconds();
    Instant expiresAt = Instant.now().plusSeconds(lifetimeSeconds);
    RefreshToken refreshToken = new RefreshToken(hash(rawToken), expiresAt, userAccount);
    repository.save(refreshToken);
    enforceSessionLimit(userAccount);
    return new IssuedRefreshToken(rawToken, expiresAt, accountInfo);
  }

  @Transactional
  public IssuedRefreshToken rotate(String rawToken) {
    RefreshToken existing = requireActiveToken(rawToken);
    existing.revoke();
    repository.save(existing);
    return issue(existing.getUser());
  }

  @Transactional
  public void revoke(String rawToken) {
    RefreshToken token = requireActiveToken(rawToken);
    token.revoke();
    repository.save(token);
  }

  @Transactional
  public void revokeAll(UserAccountInfo accountInfo) {
    UserAccount userAccount = resolveUserAccount(accountInfo);
    repository.deleteByUser(userAccount);
  }

  private UserAccount resolveUserAccount(UserAccountInfo accountInfo) {
    if (accountInfo instanceof UserAccount ua) {
      return ua;
    }
    return userAccountRepository.findByUsername(accountInfo.getUsername())
        .orElseThrow(InvalidCredentialsException::new);
  }

  private void enforceSessionLimit(UserAccount userAccount) {
    int maxSessions = sessionPolicyProperties.getMaxActiveSessions();
    if (maxSessions <= 0) {
      return;
    }
    var tokens = repository.findByUserOrderByCreatedAtAsc(userAccount);
    int activeTokens =
        (int) tokens.stream().filter(token -> !token.isRevoked() && !token.isExpired()).count();
    int excess = activeTokens - maxSessions;
    if (excess <= 0) {
      return;
    }
    for (RefreshToken token : tokens) {
      if (excess <= 0) {
        break;
      }
      if (!token.isRevoked() && !token.isExpired()) {
        token.revoke();
        repository.save(token);
        excess--;
      }
    }
  }

  private RefreshToken requireActiveToken(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      throw new InvalidCredentialsException();
    }
    return repository
        .findByTokenHash(hash(rawToken))
        .filter(token -> !token.isRevoked() && !token.isExpired())
        .orElseThrow(InvalidCredentialsException::new);
  }

  private String generateTokenValue() {
    byte[] bytes = new byte[64];
    secureRandom.nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }

  private String hash(String raw) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashed);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 algorithm not available", exception);
    }
  }

  @SuppressFBWarnings(
      value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
      justification = "Value object holds domain reference for downstream usage")
  public record IssuedRefreshToken(String value, Instant expiresAt, UserAccountInfo user) {}
}
