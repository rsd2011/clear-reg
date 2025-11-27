package com.example.admin.permission.spi;

/**
 * 조직 정책 조회를 위한 SPI.
 * auth 모듈에서 구현체를 제공합니다.
 */
public interface OrganizationPolicyProvider {

  /**
   * 조직의 기본 권한 그룹 코드를 반환합니다.
   *
   * @param organizationCode 조직 코드
   * @return 기본 권한 그룹 코드
   */
  String defaultPermissionGroup(String organizationCode);
}
