package com.example.auth.sso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.auth.InvalidCredentialsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MockSsoClient 테스트")
class MockSsoClientTest {

  private final MockSsoClient client = new MockSsoClient();

  @Test
  @DisplayName("Given 접두사가 포함된 토큰 When resolve 호출 Then 접두사를 제거한다")
  void givenTokenWhenResolveThenStrip() {
    assertThat(client.resolveUsername("SSO-user")).isEqualTo("user");
  }

  @Test
  @DisplayName("Given 잘못된 토큰 When resolve 호출 Then InvalidCredentialsException을 던진다")
  void givenInvalidTokenWhenResolveThenThrow() {
    assertThatThrownBy(() -> client.resolveUsername("invalid"))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  @DisplayName("Given null 토큰 When resolve 호출 Then InvalidCredentialsException을 던진다")
  void givenNullTokenWhenResolveThenThrow() {
    assertThatThrownBy(() -> client.resolveUsername(null))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  @DisplayName("resolveSsoId default 메서드는 토큰 자체를 반환한다")
  void resolveSsoId_returnsTokenAsIs() {
    SsoClient ssoClient = client;
    assertThat(ssoClient.resolveSsoId("any-token")).isEqualTo("any-token");
  }
}
