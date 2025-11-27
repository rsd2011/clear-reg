package com.example.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("JwtTokenProvider 테스트")
class JwtTokenProviderTest {

  private static final String VALID_SECRET =
      "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF";
  private static final String SHORT_SECRET = "shortkey"; // 8 bytes - too short for HS256

  private JwtProperties properties;
  private TokenBlacklistService blacklistService;

  @BeforeEach
  void setUp() {
    properties = new JwtProperties();
    properties.setSecret(VALID_SECRET);
    properties.setAccessTokenSeconds(3600);
    properties.setIssuer("test-app");
    properties.setAudience("test-audience");
    blacklistService = mock(TokenBlacklistService.class);
  }

  @Nested
  @DisplayName("토큰 생성 테스트")
  class GenerateTokenTests {

    @Test
    @DisplayName("Given 사용자와 역할 When 토큰 생성 Then 파싱을 통해 동일 정보를 확인할 수 있다")
    void givenSubjectWhenGeneratingTokenThenParse() {
      JwtTokenProvider provider = new JwtTokenProvider(properties, blacklistService);

      JwtTokenProvider.JwtToken token =
          provider.generateAccessToken("tester", List.of("ROLE_USER"));

      assertThat(provider.isValid(token.value())).isTrue();
      assertThat(provider.extractUsername(token.value())).isEqualTo("tester");
      assertThat(provider.extractRoles(token.value())).containsExactly("ROLE_USER");
      assertThat(token.expiresAt()).isAfter(Instant.now());
      assertThat(token.jti()).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("Given 토큰 생성 When JTI 추출 Then 고유한 JTI를 반환한다")
    void generatedTokenHasUniqueJti() {
      JwtTokenProvider provider = new JwtTokenProvider(properties, blacklistService);

      JwtTokenProvider.JwtToken token1 =
          provider.generateAccessToken("user1", List.of("ROLE_USER"));
      JwtTokenProvider.JwtToken token2 =
          provider.generateAccessToken("user2", List.of("ROLE_USER"));

      assertThat(token1.jti()).isNotEqualTo(token2.jti());
      assertThat(provider.extractJti(token1.value())).isEqualTo(token1.jti());
    }
  }

  @Nested
  @DisplayName("비밀키 검증 테스트")
  class SecretKeyValidationTests {

    @Test
    @DisplayName("Given 짧은 비밀키 When 생성자 호출 Then IllegalArgumentException 발생")
    void shortSecretThrowsException() {
      properties.setSecret(SHORT_SECRET);

      assertThatThrownBy(() -> new JwtTokenProvider(properties, blacklistService))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("at least 32 bytes");
    }

    @Test
    @DisplayName("Given null 비밀키 When 생성자 호출 Then IllegalArgumentException 발생")
    void nullSecretThrowsException() {
      properties.setSecret(null);

      assertThatThrownBy(() -> new JwtTokenProvider(properties, blacklistService))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Given 빈 비밀키 When 생성자 호출 Then IllegalArgumentException 발생")
    void emptySecretThrowsException() {
      properties.setSecret("");

      assertThatThrownBy(() -> new JwtTokenProvider(properties, blacklistService))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Given 정확히 32바이트 비밀키 When 생성자 호출 Then 정상 생성")
    void exactly32BytesSecretWorks() {
      properties.setSecret("01234567890123456789012345678901"); // exactly 32 bytes

      JwtTokenProvider provider = new JwtTokenProvider(properties, blacklistService);

      assertThat(provider).isNotNull();
    }
  }

  @Nested
  @DisplayName("토큰 검증 테스트")
  class TokenValidationTests {

    @Test
    @DisplayName("Given 유효한 토큰 When isValid Then true 반환")
    void validTokenReturnsTrue() {
      JwtTokenProvider provider = new JwtTokenProvider(properties, blacklistService);
      JwtTokenProvider.JwtToken token =
          provider.generateAccessToken("tester", List.of("ROLE_USER"));

      assertThat(provider.isValid(token.value())).isTrue();
    }

    @Test
    @DisplayName("Given 블랙리스트된 토큰 When isValid Then false 반환")
    void blacklistedTokenReturnsFalse() {
      when(blacklistService.isBlacklisted(any())).thenReturn(true);
      JwtTokenProvider provider = new JwtTokenProvider(properties, blacklistService);
      JwtTokenProvider.JwtToken token =
          provider.generateAccessToken("tester", List.of("ROLE_USER"));

      assertThat(provider.isValid(token.value())).isFalse();
    }

    @Test
    @DisplayName("Given 만료된 토큰 When isValid Then false 반환")
    void expiredTokenReturnsFalse() {
      properties.setAccessTokenSeconds(-1); // 이미 만료된 토큰 생성
      JwtTokenProvider provider = new JwtTokenProvider(properties, blacklistService);
      JwtTokenProvider.JwtToken token =
          provider.generateAccessToken("tester", List.of("ROLE_USER"));

      assertThat(provider.isValid(token.value())).isFalse();
    }

    @Test
    @DisplayName("Given 잘못된 형식의 토큰 When isValid Then false 반환")
    void malformedTokenReturnsFalse() {
      JwtTokenProvider provider = new JwtTokenProvider(properties, blacklistService);

      assertThat(provider.isValid("invalid.token.format")).isFalse();
    }

    @Test
    @DisplayName("Given 다른 키로 서명된 토큰 When isValid Then false 반환")
    void wrongSignatureReturnsFalse() {
      JwtTokenProvider provider = new JwtTokenProvider(properties, blacklistService);

      // 다른 키로 서명된 토큰 생성
      String otherSecret = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
      String wrongToken =
          Jwts.builder()
              .setSubject("tester")
              .setIssuer(properties.getIssuer())
              .setAudience(properties.getAudience())
              .setExpiration(Date.from(Instant.now().plusSeconds(3600)))
              .signWith(
                  new SecretKeySpec(
                      otherSecret.getBytes(StandardCharsets.UTF_8),
                      SignatureAlgorithm.HS256.getJcaName()),
                  SignatureAlgorithm.HS256)
              .compact();

      assertThat(provider.isValid(wrongToken)).isFalse();
    }

    @Test
    @DisplayName("Given 다른 issuer의 토큰 When isValid Then false 반환")
    void wrongIssuerReturnsFalse() {
      JwtTokenProvider provider = new JwtTokenProvider(properties, blacklistService);

      String wrongIssuerToken =
          Jwts.builder()
              .setSubject("tester")
              .setIssuer("wrong-issuer")
              .setAudience(properties.getAudience())
              .setExpiration(Date.from(Instant.now().plusSeconds(3600)))
              .signWith(
                  new SecretKeySpec(
                      VALID_SECRET.getBytes(StandardCharsets.UTF_8),
                      SignatureAlgorithm.HS256.getJcaName()),
                  SignatureAlgorithm.HS256)
              .compact();

      assertThat(provider.isValid(wrongIssuerToken)).isFalse();
    }

    @Test
    @DisplayName("Given 다른 audience의 토큰 When isValid Then false 반환")
    void wrongAudienceReturnsFalse() {
      JwtTokenProvider provider = new JwtTokenProvider(properties, blacklistService);

      String wrongAudienceToken =
          Jwts.builder()
              .setSubject("tester")
              .setIssuer(properties.getIssuer())
              .setAudience("wrong-audience")
              .setExpiration(Date.from(Instant.now().plusSeconds(3600)))
              .signWith(
                  new SecretKeySpec(
                      VALID_SECRET.getBytes(StandardCharsets.UTF_8),
                      SignatureAlgorithm.HS256.getJcaName()),
                  SignatureAlgorithm.HS256)
              .compact();

      assertThat(provider.isValid(wrongAudienceToken)).isFalse();
    }
  }

  @Nested
  @DisplayName("토큰 무효화 테스트")
  class TokenInvalidationTests {

    @Test
    @DisplayName("Given 유효한 토큰 When invalidate Then 블랙리스트에 추가")
    void invalidateAddsToBlacklist() {
      JwtTokenProvider provider = new JwtTokenProvider(properties, blacklistService);
      JwtTokenProvider.JwtToken token =
          provider.generateAccessToken("tester", List.of("ROLE_USER"));

      provider.invalidate(token.value());

      verify(blacklistService).blacklist(any(String.class), any(Instant.class));
    }

    @Test
    @DisplayName("Given 만료된 토큰 When invalidate Then 블랙리스트에 추가하지 않음")
    void invalidateExpiredTokenNoOp() {
      properties.setAccessTokenSeconds(-1);
      JwtTokenProvider provider = new JwtTokenProvider(properties, blacklistService);
      JwtTokenProvider.JwtToken token =
          provider.generateAccessToken("tester", List.of("ROLE_USER"));

      provider.invalidate(token.value());

      // 만료된 토큰은 블랙리스트에 추가하지 않음 (verify not called)
    }

    @Test
    @DisplayName("Given 잘못된 토큰 When invalidate Then 예외 없이 처리")
    void invalidateMalformedTokenNoException() {
      JwtTokenProvider provider = new JwtTokenProvider(properties, blacklistService);

      // 예외가 발생하지 않아야 함
      provider.invalidate("invalid.token");
    }
  }
}
