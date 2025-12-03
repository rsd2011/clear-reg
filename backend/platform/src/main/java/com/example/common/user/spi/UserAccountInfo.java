package com.example.common.user.spi;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * 사용자 계정 정보를 나타내는 인터페이스.
 *
 * <p>이 인터페이스는 platform 모듈에서 정의되며, admin 모듈의 UserAccount 엔티티가 구현합니다.
 * auth 모듈은 이 인터페이스를 통해 사용자 정보에 접근합니다.
 *
 * <p>기존 admin/permission/spi/UserInfo를 대체합니다.
 */
public interface UserAccountInfo {

  /** 사용자 고유 ID. */
  UUID getId();

  /** 로그인에 사용되는 사용자명. */
  String getUsername();

  /** 암호화된 비밀번호. */
  String getPassword();

  /** 이메일 주소. */
  String getEmail();

  /** 소속 조직 코드. */
  String getOrganizationCode();

  /** 권한 그룹 코드. */
  String getPermissionGroupCode();

  /** SSO 시스템 ID. */
  String getSsoId();

  /** Active Directory 도메인. */
  String getActiveDirectoryDomain();

  /** 사용자 역할 목록. */
  Set<String> getRoles();

  /** 계정 활성화 여부. */
  boolean isActive();

  /** 계정 잠금 여부 (현재 시간 기준). */
  boolean isLocked();

  /** 계정 잠금 해제 시간. */
  Instant getLockedUntil();

  /** 로그인 실패 횟수. */
  int getFailedLoginAttempts();

  /** 비밀번호 마지막 변경 시간. */
  Instant getPasswordChangedAt();

  /**
   * 사번 (HR 시스템 연동용).
   *
   * <p>username과 별개로 관리되는 직원 식별자입니다.
   * HR 시스템에서 동기화된 사번 정보를 저장합니다.
   *
   * @return 사번. 설정되지 않은 경우 null 반환
   */
  default String getEmployeeId() {
    return null;
  }
}
