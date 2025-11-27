package com.example.admin.permission;

import com.example.admin.permission.spi.UserInfo;
import java.util.Set;

/**
 * 테스트용 UserInfo 구현체.
 */
public class TestUserInfo implements UserInfo {

  private final String username;
  private final String organizationCode;
  private String permissionGroupCode;
  private final Set<String> roles;

  public TestUserInfo(String username, String organizationCode, String permissionGroupCode, Set<String> roles) {
    this.username = username;
    this.organizationCode = organizationCode;
    this.permissionGroupCode = permissionGroupCode;
    this.roles = roles != null ? Set.copyOf(roles) : Set.of();
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public String getOrganizationCode() {
    return organizationCode;
  }

  @Override
  public String getPermissionGroupCode() {
    return permissionGroupCode;
  }

  @Override
  public Set<String> getRoles() {
    return roles;
  }

  public void setPermissionGroupCode(String permissionGroupCode) {
    this.permissionGroupCode = permissionGroupCode;
  }
}
