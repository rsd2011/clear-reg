package com.example.auth.sso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.auth.InvalidCredentialsException;

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
}
