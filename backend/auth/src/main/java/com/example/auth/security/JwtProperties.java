package com.example.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

  private String secret;
  private long accessTokenSeconds = 900;
  private long refreshTokenSeconds = 30 * 24 * 3600;
  private long rememberMeRefreshTokenSeconds = 30 * 24 * 3600; // 30일 (Remember Me)
  private long defaultRefreshTokenSeconds = 24 * 3600; // 1일 (일반 로그인)
  private String issuer = "clear-reg";
  private String audience = "clear-reg-api";

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

  public String getAudience() {
    return audience;
  }

  public void setAudience(String audience) {
    this.audience = audience;
  }

  public long getRememberMeRefreshTokenSeconds() {
    return rememberMeRefreshTokenSeconds;
  }

  public void setRememberMeRefreshTokenSeconds(long rememberMeRefreshTokenSeconds) {
    this.rememberMeRefreshTokenSeconds = rememberMeRefreshTokenSeconds;
  }

  public long getDefaultRefreshTokenSeconds() {
    return defaultRefreshTokenSeconds;
  }

  public void setDefaultRefreshTokenSeconds(long defaultRefreshTokenSeconds) {
    this.defaultRefreshTokenSeconds = defaultRefreshTokenSeconds;
  }
}
