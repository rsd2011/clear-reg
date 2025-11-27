package com.example.auth.security;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.SignatureException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "JWT value object intentionally exposes token data and expiry")
@Component
public class JwtTokenProvider {

  private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
  private static final int MINIMUM_SECRET_KEY_BYTES = 32; // HS256 requires at least 256 bits

  private final JwtProperties properties;
  private final Key key;
  private final TokenBlacklistService tokenBlacklistService;

  public JwtTokenProvider(JwtProperties properties, TokenBlacklistService tokenBlacklistService) {
    Assert.hasText(properties.getSecret(), "security.jwt.secret must be configured");
    byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
    Assert.isTrue(
        keyBytes.length >= MINIMUM_SECRET_KEY_BYTES,
        String.format(
            "security.jwt.secret must be at least %d bytes (256 bits) for HS256, but was %d bytes",
            MINIMUM_SECRET_KEY_BYTES, keyBytes.length));
    this.properties = properties;
    this.key = new SecretKeySpec(keyBytes, SignatureAlgorithm.HS256.getJcaName());
    this.tokenBlacklistService = tokenBlacklistService;
  }

  public JwtToken generateAccessToken(String username, Collection<String> roles) {
    return generateToken(username, roles, properties.getAccessTokenSeconds());
  }

  public JwtToken generateToken(String username, Collection<String> roles, long lifetimeSeconds) {
    String jti = UUID.randomUUID().toString();
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(lifetimeSeconds);
    String token =
        Jwts.builder()
            .setId(jti)
            .setSubject(username)
            .setIssuer(properties.getIssuer())
            .setAudience(properties.getAudience())
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiresAt))
            .claim("roles", roles)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    return new JwtToken(token, expiresAt, jti);
  }

  public String extractUsername(String token) {
    return parseClaims(token).getSubject();
  }

  @SuppressWarnings("unchecked")
  public List<String> extractRoles(String token) {
    return parseClaims(token).get("roles", List.class);
  }

  public boolean isValid(String token) {
    try {
      Claims claims = parseClaims(token);
      String jti = claims.getId();
      if (jti != null && tokenBlacklistService.isBlacklisted(jti)) {
        log.debug("Token is blacklisted: jti={}", jti);
        return false;
      }
      return true;
    } catch (ExpiredJwtException e) {
      log.debug("Token expired: {}", e.getMessage());
      return false;
    } catch (MalformedJwtException e) {
      log.warn("Malformed token detected: {}", e.getMessage());
      return false;
    } catch (SignatureException e) {
      log.warn("Invalid signature detected: {}", e.getMessage());
      return false;
    } catch (Exception e) {
      log.error("Unexpected token validation error", e);
      return false;
    }
  }

  /**
   * Extracts the JTI (JWT ID) from the token.
   *
   * @param token the JWT token
   * @return the JTI or null if not present
   */
  public String extractJti(String token) {
    return parseClaims(token).getId();
  }

  /**
   * Invalidates the token by adding its JTI to the blacklist.
   *
   * @param token the JWT token to invalidate
   */
  public void invalidate(String token) {
    try {
      Claims claims = parseClaims(token);
      String jti = claims.getId();
      if (jti != null) {
        Instant expiresAt = claims.getExpiration().toInstant();
        tokenBlacklistService.blacklist(jti, expiresAt);
        log.debug("Token invalidated: jti={}", jti);
      }
    } catch (ExpiredJwtException e) {
      // 이미 만료된 토큰은 무효화할 필요 없음
      log.debug("Token already expired, no need to blacklist");
    } catch (Exception e) {
      log.warn("Failed to invalidate token: {}", e.getMessage());
    }
  }

  private Claims parseClaims(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(key)
        .requireIssuer(properties.getIssuer())
        .requireAudience(properties.getAudience())
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  @SuppressFBWarnings(
      value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
      justification = "Token record is immutable except Instant which is immutable")
  public record JwtToken(String value, Instant expiresAt, String jti) {}
}
