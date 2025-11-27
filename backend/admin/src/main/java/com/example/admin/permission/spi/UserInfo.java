package com.example.admin.permission.spi;

import java.util.Set;

/**
 * 권한 시스템에서 사용하는 사용자 정보 인터페이스.
 * auth 모듈의 UserAccount가 이 인터페이스를 구현합니다.
 */
public interface UserInfo {

  String getUsername();

  String getOrganizationCode();

  String getPermissionGroupCode();

  Set<String> getRoles();
}
