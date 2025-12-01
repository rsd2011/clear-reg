package com.example.admin.systemconfig.dto.settings;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 인증 관련 설정.
 * <p>
 * 설정 코드: auth.settings
 * </p>
 */
public record AuthenticationSettings(
    /** 비밀번호 정책 활성화 여부 */
    @JsonProperty(defaultValue = "true")
    boolean passwordPolicyEnabled,

    /** 비밀번호 이력 관리 활성화 여부 */
    @JsonProperty(defaultValue = "true")
    boolean passwordHistoryEnabled,

    /** 비밀번호 이력 보관 개수 */
    @JsonProperty(defaultValue = "5")
    int passwordHistoryCount,

    /** 계정 잠금 활성화 여부 */
    @JsonProperty(defaultValue = "true")
    boolean accountLockEnabled,

    /** 계정 잠금까지 허용 실패 횟수 */
    @JsonProperty(defaultValue = "5")
    int accountLockThreshold,

    /** 계정 잠금 해제까지 대기 시간(분) */
    @JsonProperty(defaultValue = "30")
    int accountLockDurationMinutes,

    /** 활성화된 로그인 유형 목록 */
    List<String> enabledLoginTypes,

    /** 세션 타임아웃(분) */
    @JsonProperty(defaultValue = "30")
    int sessionTimeoutMinutes,

    /** 동시 세션 허용 여부 */
    @JsonProperty(defaultValue = "false")
    boolean concurrentSessionAllowed,

    /** 최대 동시 세션 수 */
    @JsonProperty(defaultValue = "1")
    int maxConcurrentSessions,

    /** SSO 활성화 여부 */
    @JsonProperty(defaultValue = "false")
    boolean ssoEnabled,

    /** SSO Provider (LDAP, AD, OAUTH2 등) */
    String ssoProvider
) {
  /** 기본값으로 정규화 */
  public AuthenticationSettings {
    enabledLoginTypes = enabledLoginTypes == null ? List.of("PASSWORD") : List.copyOf(enabledLoginTypes);
    if (passwordHistoryCount < 0) {
      passwordHistoryCount = 5;
    }
    if (accountLockThreshold < 1) {
      accountLockThreshold = 5;
    }
    if (accountLockDurationMinutes < 1) {
      accountLockDurationMinutes = 30;
    }
    if (sessionTimeoutMinutes < 1) {
      sessionTimeoutMinutes = 30;
    }
    if (maxConcurrentSessions < 1) {
      maxConcurrentSessions = 1;
    }
  }

  /** 기본 설정 생성 */
  public static AuthenticationSettings defaults() {
    return new AuthenticationSettings(
        true, true, 5, true, 5, 30,
        List.of("PASSWORD"), 30, false, 1, false, null
    );
  }
}
