package com.example.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.auth.domain.RefreshTokenService;
import com.example.auth.domain.UserAccount;
import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.TokenResponse;
import com.example.auth.security.JwtProperties;
import com.example.auth.security.JwtTokenProvider;
import com.example.auth.strategy.AuthenticationStrategy;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTests {

    private AuthService authService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-test-secret-test-secret");
        properties.setAccessTokenSeconds(3600);
        properties.setRefreshTokenSeconds(7200);
        JwtTokenProvider provider = new JwtTokenProvider(properties);
        this.authService = new AuthService(List.of(new StubStrategy()), provider, refreshTokenService);
    }

    @Test
    @DisplayName("Given password login When authentication succeeds Then issue access and refresh tokens")
    void givenPasswordLoginWhenAuthenticatedThenIssueTokens() {
        var user = UserAccount.builder()
                .username("test")
                .password("pw")
                .email("test@example.com")
                .roles(Set.of("ROLE_USER"))
                .build();
        var issued = new RefreshTokenService.IssuedRefreshToken("refresh-token", Instant.now().plusSeconds(3600), user);
        given(refreshTokenService.issue(any())).willReturn(issued);

        var response = authService.login(new LoginRequest(LoginType.PASSWORD, "test", "pw", null));

        assertThat(response.username()).isEqualTo("test");
        assertThat(response.type()).isEqualTo(LoginType.PASSWORD);
        assertThat(response.tokens().accessToken()).isNotBlank();
        assertThat(response.tokens().refreshToken()).isEqualTo("refresh-token");
        then(refreshTokenService).should().issue(any());
    }

    @Test
    @DisplayName("Given unsupported login type When login requested Then throw InvalidCredentialsException")
    void givenUnsupportedTypeWhenLoginThenThrow() {
        assertThatThrownBy(() -> authService.login(new LoginRequest(LoginType.SSO, null, null, null)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Given refresh token When rotate requested Then provide new token pair")
    void givenRefreshTokenWhenRotateThenReturnNewPair() {
        var user = UserAccount.builder()
                .username("test")
                .password("pw")
                .email("test@example.com")
                .roles(Set.of("ROLE_USER"))
                .build();
        var rotated = new RefreshTokenService.IssuedRefreshToken("new-refresh", Instant.now().plusSeconds(3600), user);
        given(refreshTokenService.rotate("refresh-token")).willReturn(rotated);

        TokenResponse response = authService.refreshTokens("refresh-token");

        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        assertThat(response.accessToken()).isNotBlank();
        then(refreshTokenService).should().rotate("refresh-token");
    }

    @Test
    @DisplayName("Given refresh token When logout Then revoke token")
    void givenRefreshTokenWhenLogoutThenRevoke() {
        authService.logout("refresh-token");

        then(refreshTokenService).should().revoke("refresh-token");
    }

    private static class StubStrategy implements AuthenticationStrategy {

        @Override
        public LoginType supportedType() {
            return LoginType.PASSWORD;
        }

        @Override
        public UserAccount authenticate(LoginRequest request) {
            return UserAccount.builder()
                    .username("test")
                    .password("pw")
                    .email("test@example.com")
                    .roles(Set.of("ROLE_USER"))
                    .build();
        }
    }
}
