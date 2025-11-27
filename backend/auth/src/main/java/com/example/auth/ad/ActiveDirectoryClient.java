package com.example.auth.ad;

public interface ActiveDirectoryClient {

  boolean authenticate(String username, String password);

  /**
   * 인증된 사용자의 AD 도메인을 반환한다.
   * 기본적으로 설정된 기본 도메인을 반환한다.
   */
  default String getDomain() {
    return "DEFAULT";
  }
}
