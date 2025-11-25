package com.example.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

  private String secret;
  private long accessTokenSeconds = 900;
  private long refreshTokenSeconds = 30 * 24 * 3600;
  private String issuer = "clear-reg";

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public long getAccessTokenSeconds() {
    return accessTokenSeconds;
  }

  public void setAccessTokenSeconds(long accessTokenSeconds) {
    this.accessTokenSeconds = accessTokenSeconds;
  }

  public long getRefreshTokenSeconds() {
    return refreshTokenSeconds;
  }

  public void setRefreshTokenSeconds(long refreshTokenSeconds) {
    this.refreshTokenSeconds = refreshTokenSeconds;
  }

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }
}
