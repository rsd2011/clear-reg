package com.example.auth.jit;

import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SSO/AD 로그인 시 JIT(Just-In-Time) Provisioning 설정.
 * 사용자가 존재하지 않을 때 DW 기반으로 자동 생성하거나,
 * 기존 사용자에게 SSO/AD ID를 연결하는 기능을 제어한다.
 */
@Component
@ConfigurationProperties(prefix = "security.jit")
public class JitProvisioningProperties {

  /** JIT Provisioning 활성화 여부. */
  private boolean enabled = false;

  /** 기존 사용자에게 SSO/AD ID 자동 연결 허용 여부. */
  private boolean linkExistingUsers = true;

  /** DW에 없는 사용자의 JIT 생성을 허용할지 여부. false면 DW 필수. */
  private boolean allowWithoutDwRecord = false;

  /** DW 미등록 사용자 생성 시 기본 조직 코드. */
  private String fallbackOrganizationCode = "ROOT";

  /** DW 미등록 사용자 생성 시 기본 권한 그룹 코드. */
  private String fallbackPermissionGroupCode = "DEFAULT";

  /** 신규 사용자에게 부여할 기본 역할 목록. */
  private Set<String> defaultRoles = Set.of("USER");

  /** JIT 대상 로그인 타입 (SSO, AD). 비어있으면 모든 외부 인증에 적용. */
  private Set<String> enabledLoginTypes = Set.of("SSO", "AD");

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isLinkExistingUsers() {
    return linkExistingUsers;
  }

  public void setLinkExistingUsers(boolean linkExistingUsers) {
    this.linkExistingUsers = linkExistingUsers;
  }

  public boolean isAllowWithoutDwRecord() {
    return allowWithoutDwRecord;
  }

  public void setAllowWithoutDwRecord(boolean allowWithoutDwRecord) {
    this.allowWithoutDwRecord = allowWithoutDwRecord;
  }

  public String getFallbackOrganizationCode() {
    return fallbackOrganizationCode;
  }

  public void setFallbackOrganizationCode(String fallbackOrganizationCode) {
    this.fallbackOrganizationCode = fallbackOrganizationCode;
  }

  public String getFallbackPermissionGroupCode() {
    return fallbackPermissionGroupCode;
  }

  public void setFallbackPermissionGroupCode(String fallbackPermissionGroupCode) {
    this.fallbackPermissionGroupCode = fallbackPermissionGroupCode;
  }

  public Set<String> getDefaultRoles() {
    return defaultRoles;
  }

  public void setDefaultRoles(Set<String> defaultRoles) {
    this.defaultRoles = defaultRoles == null ? Set.of() : Set.copyOf(defaultRoles);
  }

  public Set<String> getEnabledLoginTypes() {
    return enabledLoginTypes;
  }

  public void setEnabledLoginTypes(Set<String> enabledLoginTypes) {
    this.enabledLoginTypes = enabledLoginTypes == null ? Set.of() : Set.copyOf(enabledLoginTypes);
  }
}
