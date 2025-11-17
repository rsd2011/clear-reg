package com.example.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.auth.InvalidCredentialsException;
import com.example.auth.config.SessionPolicyProperties;
import com.example.auth.security.JwtProperties;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository repository;

    private RefreshTokenService service;

    private UserAccount userAccount;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("secret-key-secret-key-secret-key");
        properties.setRefreshTokenSeconds(3600);
        SessionPolicyProperties sessionPolicyProperties = new SessionPolicyProperties();
        sessionPolicyProperties.setMaxActiveSessions(1);
        service = new RefreshTokenService(repository, properties, sessionPolicyProperties);
        userAccount = UserAccount.builder()
                .username("tester")
                .password("pw")
                .email("tester@example.com")
                .roles(Set.of("ROLE_USER"))
                .build();
    }

    @Test
    @DisplayName("Given user When issuing refresh token Then persist hashed token")
    void givenUserWhenIssueThenPersistToken() {
        given(repository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenService.IssuedRefreshToken token = service.issue(userAccount);

        assertThat(token.value()).isNotBlank();
        assertThat(token.expiresAt()).isAfter(Instant.now());
        then(repository).should().save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Given valid refresh token When rotating Then revoke and return new token")
    void givenValidTokenWhenRotateThenNewToken() {
        RefreshToken existing = RefreshToken.builder()
                .tokenHash("hash")
                .expiresAt(Instant.now().plusSeconds(5000))
                .user(userAccount)
                .build();
        given(repository.findByTokenHash(anyString())).willReturn(Optional.of(existing));
        given(repository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenService.IssuedRefreshToken rotated = service.rotate("raw-token");

        assertThat(rotated.value()).isNotBlank();
        assertThat(rotated.expiresAt()).isAfter(Instant.now());
        then(repository).should().save(existing);
    }

    @Test
    @DisplayName("Given session limit When issuing Then oldest token revoked")
    void givenSessionLimitWhenIssueThenRevokeOldest() {
        given(repository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        RefreshToken existing = RefreshToken.builder()
                .tokenHash("existing")
                .expiresAt(Instant.now().plusSeconds(1000))
                .user(userAccount)
                .build();
        RefreshToken another = RefreshToken.builder()
                .tokenHash("another")
                .expiresAt(Instant.now().plusSeconds(1500))
                .user(userAccount)
                .build();
        given(repository.findByUserOrderByCreatedAtAsc(userAccount)).willReturn(java.util.List.of(existing, another));

        service.issue(userAccount);

        assertThat(existing.isRevoked()).isTrue();
        then(repository).should().save(existing);
    }

    @Test
    @DisplayName("Given invalid token When revoking Then throw InvalidCredentialsException")
    void givenInvalidTokenWhenRevokeThenThrow() {
        given(repository.findByTokenHash(anyString())).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.revoke("missing"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
