package com.example.admin.systemconfig.dto.settings;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 감사 로그 관련 설정.
 * <p>
 * 설정 코드: audit.settings
 * </p>
 */
public record AuditSettings(
    // === 기본 설정 ===

    /** 감사 로그 활성화 여부 */
    @JsonProperty(defaultValue = "true")
    boolean auditEnabled,

    /** 감사 사유 필수 입력 여부 */
    @JsonProperty(defaultValue = "true")
    boolean auditReasonRequired,

    /** 민감 API 기본 감사 활성화 */
    @JsonProperty(defaultValue = "true")
    boolean auditSensitiveApiDefaultOn,

    /** 감사 로그 보관 기간 (일) */
    @JsonProperty(defaultValue = "730")
    int auditRetentionDays,

    /** 엄격 모드 활성화 */
    @JsonProperty(defaultValue = "true")
    boolean auditStrictMode,

    /** 위험 수준 (LOW, MEDIUM, HIGH) */
    @JsonProperty(defaultValue = "MEDIUM")
    String auditRiskLevel,

    // === 마스킹 설정 ===

    /** 감사 로그 마스킹 활성화 */
    @JsonProperty(defaultValue = "true")
    boolean auditMaskingEnabled,

    /** 민감 엔드포인트 목록 */
    List<String> auditSensitiveEndpoints,

    /** 마스킹 해제 권한 역할 목록 */
    List<String> auditUnmaskRoles,

    // === 파티션 설정 ===

    /** 파티션 활성화 */
    @JsonProperty(defaultValue = "false")
    boolean auditPartitionEnabled,

    /** 파티션 생성 크론 표현식 */
    @JsonProperty(defaultValue = "0 0 2 1 * *")
    String auditPartitionCron,

    /** 파티션 사전 생성 개월 수 */
    @JsonProperty(defaultValue = "1")
    int auditPartitionPreloadMonths,

    // === 월간 리포트 설정 ===

    /** 월간 리포트 생성 활성화 */
    @JsonProperty(defaultValue = "true")
    boolean auditMonthlyReportEnabled,

    /** 월간 리포트 생성 크론 표현식 */
    @JsonProperty(defaultValue = "0 0 4 1 * *")
    String auditMonthlyReportCron,

    // === 로그 보관 설정 ===

    /** 로그 보관 정리 활성화 */
    @JsonProperty(defaultValue = "true")
    boolean auditLogRetentionEnabled,

    /** 로그 보관 정리 크론 표현식 */
    @JsonProperty(defaultValue = "0 0 3 * * *")
    String auditLogRetentionCron,

    // === 콜드 아카이브 설정 ===

    /** 콜드 아카이브 활성화 */
    @JsonProperty(defaultValue = "false")
    boolean auditColdArchiveEnabled,

    /** 콜드 아카이브 크론 표현식 */
    @JsonProperty(defaultValue = "0 30 2 2 * *")
    String auditColdArchiveCron,

    // === 보관 정리 설정 ===

    /** 보관 정리 활성화 */
    @JsonProperty(defaultValue = "true")
    boolean auditRetentionCleanupEnabled,

    /** 보관 정리 크론 표현식 */
    @JsonProperty(defaultValue = "0 30 3 * * *")
    String auditRetentionCleanupCron
) {
  /** 기본값으로 정규화 */
  public AuditSettings {
    if (auditRetentionDays < 0) {
      auditRetentionDays = 730;
    }
    auditRiskLevel = auditRiskLevel == null ? "MEDIUM" : auditRiskLevel.toUpperCase();
    auditSensitiveEndpoints = auditSensitiveEndpoints == null ? List.of() : List.copyOf(auditSensitiveEndpoints);
    auditUnmaskRoles = auditUnmaskRoles == null ? List.of() : List.copyOf(auditUnmaskRoles);
    if (auditPartitionCron == null || auditPartitionCron.isBlank()) {
      auditPartitionCron = "0 0 2 1 * *";
    }
    if (auditPartitionPreloadMonths < 0) {
      auditPartitionPreloadMonths = 1;
    }
    if (auditMonthlyReportCron == null || auditMonthlyReportCron.isBlank()) {
      auditMonthlyReportCron = "0 0 4 1 * *";
    }
    if (auditLogRetentionCron == null || auditLogRetentionCron.isBlank()) {
      auditLogRetentionCron = "0 0 3 * * *";
    }
    if (auditColdArchiveCron == null || auditColdArchiveCron.isBlank()) {
      auditColdArchiveCron = "0 30 2 2 * *";
    }
    if (auditRetentionCleanupCron == null || auditRetentionCleanupCron.isBlank()) {
      auditRetentionCleanupCron = "0 30 3 * * *";
    }
  }

  /** 기본 설정 생성 */
  public static AuditSettings defaults() {
    return new AuditSettings(
        true, true, true, 730, true, "MEDIUM",
        true, List.of(), List.of(),
        false, "0 0 2 1 * *", 1,
        true, "0 0 4 1 * *",
        true, "0 0 3 * * *",
        false, "0 30 2 2 * *",
        true, "0 30 3 * * *"
    );
  }
}
