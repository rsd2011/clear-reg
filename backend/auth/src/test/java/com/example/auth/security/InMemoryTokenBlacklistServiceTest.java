package com.example.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("InMemoryTokenBlacklistService 테스트")
class InMemoryTokenBlacklistServiceTest {

  private InMemoryTokenBlacklistService service;

  @BeforeEach
  void setUp() {
    service = new InMemoryTokenBlacklistService();
  }

  @Nested
  @DisplayName("블랙리스트 추가 테스트")
  class BlacklistTests {

    @Test
    @DisplayName("Given JTI When blacklist Then 블랙리스트에 추가된다")
    void blacklistAddsJti() {
      String jti = "test-jti";
      Instant expiresAt = Instant.now().plusSeconds(3600);

      service.blacklist(jti, expiresAt);

      assertThat(service.isBlacklisted(jti)).isTrue();
      assertThat(service.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Given null JTI When blacklist Then 무시된다")
    void blacklistNullJtiIgnored() {
      service.blacklist(null, Instant.now().plusSeconds(3600));

      assertThat(service.size()).isZero();
    }

    @Test
    @DisplayName("Given null expiresAt When blacklist Then 무시된다")
    void blacklistNullExpiresAtIgnored() {
      service.blacklist("test-jti", null);

      assertThat(service.size()).isZero();
    }
  }

  @Nested
  @DisplayName("블랙리스트 확인 테스트")
  class IsBlacklistedTests {

    @Test
    @DisplayName("Given 블랙리스트에 없는 JTI When isBlacklisted Then false 반환")
    void notBlacklistedReturnsFalse() {
      assertThat(service.isBlacklisted("unknown-jti")).isFalse();
    }

    @Test
    @DisplayName("Given null JTI When isBlacklisted Then false 반환")
    void nullJtiReturnsFalse() {
      assertThat(service.isBlacklisted(null)).isFalse();
    }

    @Test
    @DisplayName("Given 만료된 블랙리스트 항목 When isBlacklisted Then false 반환하고 제거")
    void expiredEntryReturnsFalseAndRemoved() {
      String jti = "expired-jti";
      Instant expiredAt = Instant.now().minusSeconds(1);

      service.blacklist(jti, expiredAt);

      assertThat(service.isBlacklisted(jti)).isFalse();
      assertThat(service.size()).isZero();
    }
  }

  @Nested
  @DisplayName("정리 테스트")
  class CleanupTests {

    @Test
    @DisplayName("Given 만료된 항목들 When cleanupExpired Then 제거된다")
    void cleanupRemovesExpiredEntries() {
      service.blacklist("expired1", Instant.now().minusSeconds(10));
      service.blacklist("expired2", Instant.now().minusSeconds(5));
      service.blacklist("valid", Instant.now().plusSeconds(3600));

      service.cleanupExpired();

      assertThat(service.size()).isEqualTo(1);
      assertThat(service.isBlacklisted("valid")).isTrue();
    }

    @Test
    @DisplayName("Given 빈 블랙리스트 When cleanupExpired Then 예외 없이 처리")
    void cleanupEmptyListNoException() {
      service.cleanupExpired();

      assertThat(service.size()).isZero();
    }
  }
}
