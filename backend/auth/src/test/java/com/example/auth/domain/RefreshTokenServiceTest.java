package com.example.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.admin.user.domain.UserAccount;
import com.example.admin.user.repository.UserAccountRepository;
import com.example.auth.InvalidCredentialsException;
import com.example.auth.config.SessionPolicyProperties;
import com.example.auth.domain.RefreshTokenService.IssuedRefreshToken;
import com.example.auth.security.JwtProperties;
import com.example.common.user.spi.UserAccountInfo;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("RefreshTokenService 테스트")
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  @Mock private RefreshTokenRepository repository;
  @Mock private UserAccountRepository userAccountRepository;
  @Mock private JwtProperties jwtProperties;
  @Mock private SessionPolicyProperties sessionPolicyProperties;

  private RefreshTokenService refreshTokenService;

  @BeforeEach
  void setUp() {
    refreshTokenService =
        new RefreshTokenService(
            repository, userAccountRepository, jwtProperties, sessionPolicyProperties);
  }

  @Nested
  @DisplayName("issue 메서드")
  class IssueTests {

    @Test
    @DisplayName("Given UserAccount When issue Then 토큰 발급 및 저장")
    void givenUserAccount_whenIssue_thenSaveAndReturnToken() {
      // Given
      UserAccount userAccount = createUserAccount("testuser");
      when(jwtProperties.getDefaultRefreshTokenSeconds()).thenReturn(86400L);
      when(sessionPolicyProperties.getMaxActiveSessions()).thenReturn(0);

      // When
      IssuedRefreshToken result = refreshTokenService.issue(userAccount);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.value()).isNotBlank();
      assertThat(result.expiresAt()).isAfter(Instant.now());
      assertThat(result.user()).isEqualTo(userAccount);

      ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
      verify(repository).save(captor.capture());
      assertThat(captor.getValue().getTokenHash()).isNotBlank();
    }

    @Test
    @DisplayName("Given UserAccountInfo (not UserAccount) When issue Then 리포지토리에서 조회 후 발급")
    void givenUserAccountInfo_whenIssue_thenLookupAndIssue() {
      // Given
      UserAccountInfo accountInfo = createMockAccountInfo("testuser");
      UserAccount userAccount = createUserAccount("testuser");

      when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(userAccount));
      when(jwtProperties.getDefaultRefreshTokenSeconds()).thenReturn(86400L);
      when(sessionPolicyProperties.getMaxActiveSessions()).thenReturn(0);

      // When
      IssuedRefreshToken result = refreshTokenService.issue(accountInfo);

      // Then
      assertThat(result).isNotNull();
      verify(userAccountRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Given rememberMe=true When issue Then 긴 유효기간으로 발급")
    void givenRememberMe_whenIssue_thenLongerExpiry() {
      // Given
      UserAccount userAccount = createUserAccount("testuser");
      when(jwtProperties.getRememberMeRefreshTokenSeconds()).thenReturn(604800L); // 7일
      when(sessionPolicyProperties.getMaxActiveSessions()).thenReturn(0);

      // When
      IssuedRefreshToken result = refreshTokenService.issue(userAccount, true);

      // Then
      assertThat(result.expiresAt()).isAfter(Instant.now().plusSeconds(600000L));
    }

    @Test
    @DisplayName("Given 세션 제한 초과 When issue Then 오래된 토큰 폐기")
    void givenSessionLimitExceeded_whenIssue_thenRevokeOldTokens() {
      // Given
      UserAccount userAccount = createUserAccount("testuser");
      RefreshToken activeToken1 = createActiveToken(userAccount);
      RefreshToken activeToken2 = createActiveToken(userAccount);
      RefreshToken newToken = createActiveToken(userAccount);

      when(jwtProperties.getDefaultRefreshTokenSeconds()).thenReturn(86400L);
      when(sessionPolicyProperties.getMaxActiveSessions()).thenReturn(2);
      // 새 토큰 발급 후 enforceSessionLimit에서 조회하면 3개의 토큰이 반환됨
      when(repository.findByUserOrderByCreatedAtAsc(userAccount))
          .thenReturn(List.of(activeToken1, activeToken2, newToken));

      // When
      refreshTokenService.issue(userAccount);

      // Then
      // 새 토큰 저장 + 기존 토큰 1개 폐기(revoke 후 save) = 최소 2번
      verify(repository, times(2)).save(any(RefreshToken.class));
    }
  }

  @Nested
  @DisplayName("rotate 메서드")
  class RotateTests {

    @Test
    @DisplayName("Given 유효한 토큰 When rotate Then 기존 토큰 폐기 및 새 토큰 발급")
    void givenValidToken_whenRotate_thenRevokeAndIssueNew() {
      // Given
      UserAccount userAccount = createUserAccount("testuser");
      RefreshToken existingToken = createActiveToken(userAccount);
      String rawToken = "valid-raw-token";

      when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(existingToken));
      when(jwtProperties.getDefaultRefreshTokenSeconds()).thenReturn(86400L);
      when(sessionPolicyProperties.getMaxActiveSessions()).thenReturn(0);

      // When
      IssuedRefreshToken result = refreshTokenService.rotate(rawToken);

      // Then
      assertThat(result).isNotNull();
      assertThat(existingToken.isRevoked()).isTrue();
      verify(repository, times(2)).save(any(RefreshToken.class)); // 폐기 + 새 토큰
    }

    @Test
    @DisplayName("Given 만료된 토큰 When rotate Then InvalidCredentialsException 발생")
    void givenExpiredToken_whenRotate_thenThrowException() {
      // Given
      UserAccount userAccount = createUserAccount("testuser");
      RefreshToken expiredToken = createExpiredToken(userAccount);

      when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(expiredToken));

      // When & Then
      assertThatThrownBy(() -> refreshTokenService.rotate("expired-token"))
          .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Given 이미 폐기된 토큰 When rotate Then InvalidCredentialsException 발생")
    void givenRevokedToken_whenRotate_thenThrowException() {
      // Given
      UserAccount userAccount = createUserAccount("testuser");
      RefreshToken revokedToken = createActiveToken(userAccount);
      revokedToken.revoke();

      when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(revokedToken));

      // When & Then
      assertThatThrownBy(() -> refreshTokenService.rotate("revoked-token"))
          .isInstanceOf(InvalidCredentialsException.class);
    }
  }

  @Nested
  @DisplayName("revoke 메서드")
  class RevokeTests {

    @Test
    @DisplayName("Given 유효한 토큰 When revoke Then 토큰 폐기")
    void givenValidToken_whenRevoke_thenMarkAsRevoked() {
      // Given
      UserAccount userAccount = createUserAccount("testuser");
      RefreshToken token = createActiveToken(userAccount);

      when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

      // When
      refreshTokenService.revoke("valid-token");

      // Then
      assertThat(token.isRevoked()).isTrue();
      verify(repository).save(token);
    }

    @Test
    @DisplayName("Given null 토큰 When revoke Then InvalidCredentialsException 발생")
    void givenNullToken_whenRevoke_thenThrowException() {
      // When & Then
      assertThatThrownBy(() -> refreshTokenService.revoke(null))
          .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Given 빈 토큰 When revoke Then InvalidCredentialsException 발생")
    void givenBlankToken_whenRevoke_thenThrowException() {
      // When & Then
      assertThatThrownBy(() -> refreshTokenService.revoke("   "))
          .isInstanceOf(InvalidCredentialsException.class);
    }
  }

  @Nested
  @DisplayName("revokeAll 메서드")
  class RevokeAllTests {

    @Test
    @DisplayName("Given UserAccount When revokeAll Then 해당 사용자 모든 토큰 삭제")
    void givenUserAccount_whenRevokeAll_thenDeleteAllUserTokens() {
      // Given
      UserAccount userAccount = createUserAccount("testuser");

      // When
      refreshTokenService.revokeAll(userAccount);

      // Then
      verify(repository).deleteByUser(userAccount);
    }

    @Test
    @DisplayName("Given UserAccountInfo When revokeAll Then 리포지토리에서 조회 후 토큰 삭제")
    void givenUserAccountInfo_whenRevokeAll_thenLookupAndDeleteTokens() {
      // Given
      UserAccountInfo accountInfo = createMockAccountInfo("testuser");
      UserAccount userAccount = createUserAccount("testuser");

      when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(userAccount));

      // When
      refreshTokenService.revokeAll(accountInfo);

      // Then
      verify(userAccountRepository).findByUsername("testuser");
      verify(repository).deleteByUser(userAccount);
    }
  }

  private UserAccount createUserAccount(String username) {
    return UserAccount.builder()
        .username(username)
        .password("encoded-password")
        .organizationCode("ORG001")
        .build();
  }

  private UserAccountInfo createMockAccountInfo(String username) {
    return new UserAccountInfo() {
      @Override
      public UUID getId() {
        return UUID.randomUUID();
      }

      @Override
      public String getUsername() {
        return username;
      }

      @Override
      public String getPassword() {
        return "encoded-password";
      }

      @Override
      public String getEmail() {
        return username + "@example.com";
      }

      @Override
      public java.util.Set<String> getRoles() {
        return java.util.Set.of("ROLE_USER");
      }

      @Override
      public String getOrganizationCode() {
        return "ORG001";
      }

      @Override
      public String getPermissionGroupCode() {
        return "DEFAULT_GROUP";
      }

      @Override
      public String getSsoId() {
        return null;
      }

      @Override
      public String getActiveDirectoryDomain() {
        return null;
      }

      @Override
      public boolean isActive() {
        return true;
      }

      @Override
      public boolean isLocked() {
        return false;
      }

      @Override
      public int getFailedLoginAttempts() {
        return 0;
      }

      @Override
      public Instant getLockedUntil() {
        return null;
      }

      @Override
      public Instant getPasswordChangedAt() {
        return Instant.now();
      }
    };
  }

  private RefreshToken createActiveToken(UserAccount user) {
    return new RefreshToken("hash-value", Instant.now().plusSeconds(3600), user);
  }

  private RefreshToken createExpiredToken(UserAccount user) {
    return new RefreshToken("hash-value", Instant.now().minusSeconds(3600), user);
  }
}
