package com.example.auth.security;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "JWT value object intentionally exposes token data and expiry")
@Component
public class JwtTokenProvider {

  private final JwtProperties properties;
  private final Key key;

  public JwtTokenProvider(JwtProperties properties) {
    Assert.hasText(properties.getSecret(), "security.jwt.secret must be configured");
    this.properties = properties;
    this.key =
        new SecretKeySpec(
            properties.getSecret().getBytes(StandardCharsets.UTF_8),
            SignatureAlgorithm.HS256.getJcaName());
  }

  public JwtToken generateAccessToken(String username, Collection<String> roles) {
    return generateToken(username, roles, properties.getAccessTokenSeconds());
  }

  public JwtToken generateToken(String username, Collection<String> roles, long lifetimeSeconds) {
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(lifetimeSeconds);
    String token =
        Jwts.builder()
            .setSubject(username)
            .setIssuer(properties.getIssuer())
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiresAt))
            .claim("roles", roles)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    return new JwtToken(token, expiresAt);
  }

  public String extractUsername(String token) {
    return parseClaims(token).getSubject();
  }

  public List<String> extractRoles(String token) {
    return parseClaims(token).get("roles", List.class);
  }

  public boolean isValid(String token) {
    try {
      parseClaims(token);
      return true;
    } catch (Exception exception) {
      return false;
    }
  }

  private Claims parseClaims(String token) {
    return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
  }

  @SuppressFBWarnings(
      value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
      justification = "Token record is immutable except Instant which is immutable")
  public record JwtToken(String value, Instant expiresAt) {}
}
