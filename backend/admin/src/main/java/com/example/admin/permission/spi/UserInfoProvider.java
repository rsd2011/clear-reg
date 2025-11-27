package com.example.admin.permission.spi;

/**
 * 사용자 정보 조회를 위한 SPI.
 * auth 모듈에서 구현체를 제공합니다.
 */
public interface UserInfoProvider {

  /**
   * 사용자명으로 사용자 정보를 조회합니다.
   *
   * @param username 사용자명
   * @return 사용자 정보
   * @throws RuntimeException 사용자를 찾을 수 없는 경우
   */
  UserInfo getByUsernameOrThrow(String username);
}
