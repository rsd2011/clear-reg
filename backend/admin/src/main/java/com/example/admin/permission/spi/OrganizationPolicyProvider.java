package com.example.admin.permission.spi;

import java.util.List;
import java.util.Set;

/**
 * 조직 정책 조회를 위한 SPI.
 * admin 모듈의 OrganizationPolicyService가 구현합니다.
 */
public interface OrganizationPolicyProvider {

  /**
   * 조직의 기본 권한 그룹 코드를 반환합니다.
   *
   * @param organizationCode 조직 코드
   * @return 기본 권한 그룹 코드
   */
  String defaultPermissionGroup(String organizationCode);

  /**
   * 조직의 승인 흐름을 반환합니다.
   *
   * @param organizationCode 조직 코드
   * @return 승인 그룹 코드 목록 (순서 유지)
   */
  List<String> approvalFlow(String organizationCode);

  /**
   * 조직에서 사용 가능한 추가 권한 그룹들을 반환합니다.
   *
   * @param organizationCode 조직 코드
   * @return 추가 권한 그룹 코드 집합
   */
  Set<String> availableGroups(String organizationCode);
}
