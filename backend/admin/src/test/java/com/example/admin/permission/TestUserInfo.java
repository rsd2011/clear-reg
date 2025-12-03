package com.example.admin.permission;

import com.example.common.user.spi.UserAccountInfo;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * 테스트용 UserAccountInfo 구현체.
 */
public class TestUserInfo implements UserAccountInfo {

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
  public UUID getId() {
    return UUID.randomUUID();
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public String getPassword() {
    return "test-password";
  }

  @Override
  public String getEmail() {
    return username + "@test.com";
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
  public String getSsoId() {
    return null;
  }

  @Override
  public String getActiveDirectoryDomain() {
    return null;
  }

  @Override
  public Set<String> getRoles() {
    return roles;
  }

  @Override
  public boolean isActive() {
    return true;
  }

  @Override
  public boolean isLocked() {
    return false;
  }

  @Override
  public Instant getLockedUntil() {
    return null;
  }

  @Override
  public int getFailedLoginAttempts() {
    return 0;
  }

  @Override
  public Instant getPasswordChangedAt() {
    return Instant.now();
  }

  public void setPermissionGroupCode(String permissionGroupCode) {
    this.permissionGroupCode = permissionGroupCode;
  }
}
