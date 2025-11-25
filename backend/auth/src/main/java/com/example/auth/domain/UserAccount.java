package com.example.auth.domain;

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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Entity exposes immutable views; references managed by JPA")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class UserAccount extends PrimaryKeyEntity {

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

  @Builder
  public UserAccount(
      String username,
      String password,
      String email,
      Set<String> roles,
      String organizationCode,
      String permissionGroupCode) {
    this.username = username;
    this.password = password;
    this.email = email;
    if (roles != null) {
      this.roles = new HashSet<>(roles);
    }
    this.active = true;
    this.organizationCode = organizationCode != null ? organizationCode : "ROOT";
    this.permissionGroupCode = permissionGroupCode != null ? permissionGroupCode : "DEFAULT";
  }

  public void setSsoId(String ssoId) {
    this.ssoId = ssoId;
  }

  public void setActiveDirectoryDomain(String activeDirectoryDomain) {
    this.activeDirectoryDomain = activeDirectoryDomain;
  }

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

  public boolean isActive() {
    return active;
  }

  public int getFailedLoginAttempts() {
    return failedLoginAttempts;
  }

  public Instant getLockedUntil() {
    return lockedUntil;
  }

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

  public Instant getPasswordChangedAt() {
    return passwordChangedAt;
  }

  public String getOrganizationCode() {
    return organizationCode;
  }

  public void updateOrganizationCode(String organizationCode) {
    this.organizationCode = organizationCode;
  }

  public String getPermissionGroupCode() {
    return permissionGroupCode;
  }

  public void updatePermissionGroupCode(String permissionGroupCode) {
    this.permissionGroupCode = permissionGroupCode;
  }
}
