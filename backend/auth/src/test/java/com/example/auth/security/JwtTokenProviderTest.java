package com.example.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    @Test
    @DisplayName("Given subject and roles When generating token Then it can be parsed back")
    void givenSubjectWhenGeneratingTokenThenParse() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF");
        properties.setAccessTokenSeconds(3600);
        properties.setIssuer("test-app");
        JwtTokenProvider provider = new JwtTokenProvider(properties);

        JwtTokenProvider.JwtToken token = provider.generateAccessToken("tester", List.of("ROLE_USER"));

        assertThat(provider.isValid(token.value())).isTrue();
        assertThat(provider.extractUsername(token.value())).isEqualTo("tester");
        assertThat(provider.extractRoles(token.value())).containsExactly("ROLE_USER");
        assertThat(token.expiresAt()).isAfter(java.time.Instant.now());
    }
}
