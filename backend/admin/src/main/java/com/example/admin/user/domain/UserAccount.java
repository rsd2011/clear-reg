package com.example.admin.user.domain;

import com.example.common.user.spi.UserAccountInfo;
import com.example.common.jpa.PrimaryKeyEntity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 계정 엔티티.
 *
 * <p>users 테이블에 저장되며, 인증 및 권한 시스템에서 사용됩니다.
 * UserAccountInfo 인터페이스를 구현하여 platform SPI를 통해 접근 가능합니다.
 */
@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Entity exposes immutable views; references managed by JPA")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class UserAccount extends PrimaryKeyEntity implements UserAccountInfo {

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false)
  private String password;

  private String email;

  @Column(name = "organization_code", nullable = false)
  private String organizationCode;

  @Column(name = "permission_group_code", nullable = false)
  private String permissionGroupCode;

  @Column(name = "sso_id")
  private String ssoId;

  @Column(name = "ad_domain")
  private String activeDirectoryDomain;

  /**
   * 사번 (HR 시스템 연동용).
   *
   * <p>username과 별개로 관리되는 직원 식별자입니다.
   */
  @Column(name = "employee_id", unique = true)
  private String employeeId;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "role")
  @Getter(AccessLevel.NONE)
  private Set<String> roles = new HashSet<>();

  @Column(nullable = false)
  private boolean active = true;

  @Column(name = "failed_attempts", nullable = false)
  private int failedLoginAttempts = 0;

  @Column(name = "locked_until")
  private Instant lockedUntil;

  @Column(name = "password_changed_at")
  private Instant passwordChangedAt = Instant.now();

  @Column(name = "last_login_at")
  private Instant lastLoginAt;

  @Builder
  public UserAccount(
      String username,
      String password,
      String email,
      Set<String> roles,
      String organizationCode,
      String permissionGroupCode,
      String employeeId) {
    this.username = username;
    this.password = password;
    this.email = email;
    if (roles != null) {
      this.roles = new HashSet<>(roles);
    }
    this.active = true;
    this.organizationCode = organizationCode != null ? organizationCode : "ROOT";
    this.permissionGroupCode = permissionGroupCode != null ? permissionGroupCode : "DEFAULT";
    this.employeeId = employeeId;
  }

  /** SSO 계정 식별자를 연결한다. 한번만 설정하도록 guard. */
  public void linkSsoId(String ssoId) {
    if (this.ssoId != null && !this.ssoId.equals(ssoId)) {
      throw new IllegalStateException("SSO ID already linked");
    }
    this.ssoId = ssoId;
  }

  /** AD 도메인을 지정한다. */
  public void assignActiveDirectoryDomain(String activeDirectoryDomain) {
    this.activeDirectoryDomain = activeDirectoryDomain;
  }

  @Override
  public UUID getId() {
    return super.getId();
  }

  @Override
  public Set<String> getRoles() {
    return Collections.unmodifiableSet(roles);
  }

  public void updatePassword(String encodedPassword) {
    this.password = encodedPassword;
    this.passwordChangedAt = Instant.now();
  }

  public void updateEmail(String email) {
    this.email = email;
  }

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  public int getFailedLoginAttempts() {
    return failedLoginAttempts;
  }

  @Override
  public Instant getLockedUntil() {
    return lockedUntil;
  }

  @Override
  public boolean isLocked() {
    return lockedUntil != null && lockedUntil.isAfter(Instant.now());
  }

  public void deactivate() {
    this.active = false;
  }

  public void activate() {
    this.active = true;
    this.lockedUntil = null;
    this.failedLoginAttempts = 0;
  }

  public void incrementFailedAttempt() {
    this.failedLoginAttempts++;
  }

  public void resetFailedAttempts() {
    this.failedLoginAttempts = 0;
  }

  public void lockUntil(Instant instant) {
    this.lockedUntil = instant;
  }

  @Override
  public Instant getPasswordChangedAt() {
    return passwordChangedAt;
  }

  @Override
  public String getOrganizationCode() {
    return organizationCode;
  }

  public void updateOrganizationCode(String organizationCode) {
    this.organizationCode = organizationCode;
  }

  @Override
  public String getPermissionGroupCode() {
    return permissionGroupCode;
  }

  public void updatePermissionGroupCode(String permissionGroupCode) {
    this.permissionGroupCode = permissionGroupCode;
  }

  @Override
  public String getEmployeeId() {
    return employeeId;
  }

  /**
   * 사번을 설정합니다.
   *
   * @param employeeId 사번
   */
  public void updateEmployeeId(String employeeId) {
    this.employeeId = employeeId;
  }

  /**
   * 마지막 로그인 시간을 반환합니다.
   *
   * @return 마지막 로그인 시간
   */
  public Instant getLastLoginAt() {
    return lastLoginAt;
  }

  /**
   * 마지막 로그인 시간을 업데이트합니다.
   *
   * @param lastLoginAt 마지막 로그인 시간
   */
  public void updateLastLoginAt(Instant lastLoginAt) {
    this.lastLoginAt = lastLoginAt;
  }

  /**
   * 계정 잠금을 해제합니다.
   */
  public void unlock() {
    this.lockedUntil = null;
    this.failedLoginAttempts = 0;
  }
}
