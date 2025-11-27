package com.example.auth.sso;

public interface SsoClient {

  String resolveUsername(String token);

  /**
   * SSO 토큰에서 SSO 시스템 고유 ID를 추출한다.
   * 기본적으로 토큰 자체를 ID로 사용한다.
   */
  default String resolveSsoId(String token) {
    return token;
  }
}
