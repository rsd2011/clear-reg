package com.example.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JwtTokenProvider 테스트")
class JwtTokenProviderTest {

    @Test
    @DisplayName("Given 사용자와 역할 When 토큰 생성 Then 파싱을 통해 동일 정보를 확인할 수 있다")
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
