package com.example.auth.sso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.auth.InvalidCredentialsException;

@DisplayName("MockSsoClient")
class MockSsoClientTest {

    private final MockSsoClient client = new MockSsoClient();

    @Test
    @DisplayName("Given prefixed token When resolve Then strip prefix")
    void givenTokenWhenResolveThenStrip() {
        assertThat(client.resolveUsername("SSO-user")).isEqualTo("user");
    }

    @Test
    @DisplayName("Given invalid token When resolve Then throw")
    void givenInvalidTokenWhenResolveThenThrow() {
        assertThatThrownBy(() -> client.resolveUsername("invalid"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
