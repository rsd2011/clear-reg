package com.example.common.user.spi;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 계정 조회 및 관리를 위한 SPI.
 *
 * <p>이 인터페이스는 platform 모듈에서 정의되며, admin 모듈에서 구현체를 제공합니다.
 * auth 모듈은 이 인터페이스를 통해 사용자 정보를 조회하고 계정 상태를 관리합니다.
 *
 * <p>기존 admin/permission/spi/UserInfoProvider를 대체합니다.
 */
public interface UserAccountProvider {

  // ========== 조회 메서드 ==========

  /**
   * 사용자명으로 사용자 정보를 조회합니다.
   *
   * @param username 사용자명
   * @return 사용자 정보
   * @throws RuntimeException 사용자를 찾을 수 없는 경우
   */
  UserAccountInfo getByUsernameOrThrow(String username);

  /**
   * 사용자명으로 사용자 정보를 조회합니다.
   *
   * @param username 사용자명
   * @return 사용자 정보 (Optional)
   */
  Optional<UserAccountInfo> findByUsername(String username);

  /**
   * SSO ID로 사용자 정보를 조회합니다.
   *
   * @param ssoId SSO 시스템 ID
   * @return 사용자 정보 (Optional)
   */
  Optional<UserAccountInfo> findBySsoId(String ssoId);

  /**
   * 권한 그룹 코드 목록에 해당하는 사용자들을 조회합니다.
   *
   * @param codes 권한 그룹 코드 목록
   * @return 사용자 목록
   */
  List<? extends UserAccountInfo> findByPermissionGroupCodeIn(List<String> codes);

  // ========== 비밀번호 검증 ==========

  /**
   * 비밀번호가 일치하는지 확인합니다.
   *
   * @param username 사용자명
   * @param rawPassword 평문 비밀번호
   * @return 일치 여부
   */
  boolean passwordMatches(String username, String rawPassword);

  // ========== 계정 상태 관리 ==========

  /**
   * 로그인 실패 횟수를 증가시킵니다.
   *
   * @param username 사용자명
   */
  void incrementFailedAttempt(String username);

  /**
   * 로그인 실패 횟수를 초기화합니다.
   *
   * @param username 사용자명
   */
  void resetFailedAttempts(String username);

  /**
   * 계정을 특정 시간까지 잠급니다.
   *
   * @param username 사용자명
   * @param until 잠금 해제 시간
   */
  void lockUntil(String username, Instant until);

  /**
   * 계정을 활성화합니다.
   *
   * @param username 사용자명
   */
  void activate(String username);

  /**
   * 계정을 비활성화합니다.
   *
   * @param username 사용자명
   */
  void deactivate(String username);

  // ========== 비밀번호 변경 ==========

  /**
   * 비밀번호를 변경합니다.
   *
   * @param username 사용자명
   * @param encodedPassword 암호화된 새 비밀번호
   */
  void updatePassword(String username, String encodedPassword);
}
