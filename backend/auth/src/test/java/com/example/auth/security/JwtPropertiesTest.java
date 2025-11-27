package com.example.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JwtProperties 테스트")
class JwtPropertiesTest {

  @Test
  @DisplayName("Given JwtProperties When getter/setter Then 값이 정상적으로 설정된다")
  void gettersAndSetters() {
    JwtProperties properties = new JwtProperties();

    properties.setSecret("test-secret");
    properties.setAccessTokenSeconds(1800);
    properties.setRefreshTokenSeconds(86400);
    properties.setRememberMeRefreshTokenSeconds(2592000);
    properties.setDefaultRefreshTokenSeconds(43200);
    properties.setIssuer("test-issuer");
    properties.setAudience("test-audience");

    assertThat(properties.getSecret()).isEqualTo("test-secret");
    assertThat(properties.getAccessTokenSeconds()).isEqualTo(1800);
    assertThat(properties.getRefreshTokenSeconds()).isEqualTo(86400);
    assertThat(properties.getRememberMeRefreshTokenSeconds()).isEqualTo(2592000);
    assertThat(properties.getDefaultRefreshTokenSeconds()).isEqualTo(43200);
    assertThat(properties.getIssuer()).isEqualTo("test-issuer");
    assertThat(properties.getAudience()).isEqualTo("test-audience");
  }

  @Test
  @DisplayName("Given 기본값 When 생성 Then 기본값이 설정된다")
  void defaultValues() {
    JwtProperties properties = new JwtProperties();

    assertThat(properties.getAccessTokenSeconds()).isEqualTo(900);
    assertThat(properties.getRefreshTokenSeconds()).isEqualTo(30 * 24 * 3600);
    assertThat(properties.getRememberMeRefreshTokenSeconds()).isEqualTo(30 * 24 * 3600);
    assertThat(properties.getDefaultRefreshTokenSeconds()).isEqualTo(24 * 3600);
    assertThat(properties.getIssuer()).isEqualTo("clear-reg");
    assertThat(properties.getAudience()).isEqualTo("clear-reg-api");
  }
}
