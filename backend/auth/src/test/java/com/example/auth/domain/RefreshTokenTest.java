package com.example.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.admin.user.domain.UserAccount;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RefreshToken 엔티티 테스트")
class RefreshTokenTest {

  @Nested
  @DisplayName("생성자 테스트")
  class ConstructorTests {

    @Test
    @DisplayName("Given 유효한 파라미터 When 생성 Then RefreshToken 생성")
    void givenValidParams_whenCreate_thenRefreshTokenCreated() {
      // Given
      UserAccount user = createUserAccount("testuser");
      String tokenHash = "sha256-hash-value";
      Instant expiresAt = Instant.now().plusSeconds(3600);

      // When
      RefreshToken token = new RefreshToken(tokenHash, expiresAt, user);

      // Then
      assertThat(token.getTokenHash()).isEqualTo(tokenHash);
      assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
      assertThat(token.getUser()).isEqualTo(user);
      assertThat(token.isRevoked()).isFalse();
      assertThat(token.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Given Builder When 생성 Then RefreshToken 생성")
    void givenBuilder_whenCreate_thenRefreshTokenCreated() {
      // Given
      UserAccount user = createUserAccount("testuser");
      String tokenHash = "sha256-hash-value";
      Instant expiresAt = Instant.now().plusSeconds(3600);

      // When
      RefreshToken token =
          RefreshToken.builder().tokenHash(tokenHash).expiresAt(expiresAt).user(user).build();

      // Then
      assertThat(token.getTokenHash()).isEqualTo(tokenHash);
      assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
      assertThat(token.getUser()).isEqualTo(user);
    }
  }

  @Nested
  @DisplayName("isExpired 메서드")
  class IsExpiredTests {

    @Test
    @DisplayName("Given 미래 만료 시간 When isExpired Then false 반환")
    void givenFutureExpiry_whenIsExpired_thenReturnFalse() {
      // Given
      UserAccount user = createUserAccount("testuser");
      Instant futureExpiry = Instant.now().plusSeconds(3600);
      RefreshToken token = new RefreshToken("hash", futureExpiry, user);

      // When
      boolean result = token.isExpired();

      // Then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Given 과거 만료 시간 When isExpired Then true 반환")
    void givenPastExpiry_whenIsExpired_thenReturnTrue() {
      // Given
      UserAccount user = createUserAccount("testuser");
      Instant pastExpiry = Instant.now().minusSeconds(3600);
      RefreshToken token = new RefreshToken("hash", pastExpiry, user);

      // When
      boolean result = token.isExpired();

      // Then
      assertThat(result).isTrue();
    }
  }

  @Nested
  @DisplayName("revoke 메서드")
  class RevokeTests {

    @Test
    @DisplayName("Given 활성 토큰 When revoke Then revoked=true")
    void givenActiveToken_whenRevoke_thenRevokedTrue() {
      // Given
      UserAccount user = createUserAccount("testuser");
      RefreshToken token = new RefreshToken("hash", Instant.now().plusSeconds(3600), user);
      assertThat(token.isRevoked()).isFalse();

      // When
      token.revoke();

      // Then
      assertThat(token.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("Given 이미 폐기된 토큰 When revoke Then 여전히 revoked=true")
    void givenRevokedToken_whenRevoke_thenStillRevoked() {
      // Given
      UserAccount user = createUserAccount("testuser");
      RefreshToken token = new RefreshToken("hash", Instant.now().plusSeconds(3600), user);
      token.revoke();

      // When
      token.revoke();

      // Then
      assertThat(token.isRevoked()).isTrue();
    }
  }

  private UserAccount createUserAccount(String username) {
    return UserAccount.builder()
        .username(username)
        .password("encoded-password")
        .organizationCode("ORG001")
        .build();
  }
}
