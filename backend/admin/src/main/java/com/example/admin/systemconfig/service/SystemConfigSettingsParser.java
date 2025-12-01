package com.example.admin.systemconfig.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.example.admin.systemconfig.dto.settings.AuditSettings;
import com.example.admin.systemconfig.dto.settings.AuthenticationSettings;
import com.example.admin.systemconfig.dto.settings.FileUploadSettings;
import com.example.common.policy.AuditPartitionSettings;
import com.example.common.policy.PolicyToggleSettings;
import com.example.common.schedule.BatchJobCode;
import com.example.common.schedule.BatchJobDefaults;
import com.example.common.schedule.BatchJobSchedule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * SystemConfig YAML을 Settings 객체로 변환하는 파서.
 * <p>
 * 설정 코드별로 다른 Settings 객체로 변환합니다:
 * <ul>
 *   <li>auth.settings → AuthenticationSettings</li>
 *   <li>file.settings → FileUploadSettings</li>
 *   <li>audit.settings → AuditSettings</li>
 * </ul>
 * </p>
 */
@Component
public class SystemConfigSettingsParser {

  public static final String AUTH_CONFIG_CODE = "auth.settings";
  public static final String FILE_CONFIG_CODE = "file.settings";
  public static final String AUDIT_CONFIG_CODE = "audit.settings";

  private final ObjectMapper yamlMapper;

  public SystemConfigSettingsParser(
      @Qualifier("yamlObjectMapper") ObjectMapper yamlMapper) {
    this.yamlMapper = yamlMapper;
  }

  /**
   * YAML 문자열을 AuthenticationSettings로 파싱합니다.
   *
   * @param yaml YAML 문자열
   * @return AuthenticationSettings
   * @throws IllegalArgumentException 파싱 실패 시
   */
  public AuthenticationSettings parseAuthSettings(String yaml) {
    if (yaml == null || yaml.isBlank()) {
      return AuthenticationSettings.defaults();
    }
    try {
      return yamlMapper.readValue(yaml, AuthenticationSettings.class);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("인증 설정 YAML 파싱 실패", e);
    }
  }

  /**
   * YAML 문자열을 FileUploadSettings로 파싱합니다.
   *
   * @param yaml YAML 문자열
   * @return FileUploadSettings
   * @throws IllegalArgumentException 파싱 실패 시
   */
  public FileUploadSettings parseFileSettings(String yaml) {
    if (yaml == null || yaml.isBlank()) {
      return FileUploadSettings.defaults();
    }
    try {
      return yamlMapper.readValue(yaml, FileUploadSettings.class);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("파일 설정 YAML 파싱 실패", e);
    }
  }

  /**
   * YAML 문자열을 AuditSettings로 파싱합니다.
   *
   * @param yaml YAML 문자열
   * @return AuditSettings
   * @throws IllegalArgumentException 파싱 실패 시
   */
  public AuditSettings parseAuditSettings(String yaml) {
    if (yaml == null || yaml.isBlank()) {
      return AuditSettings.defaults();
    }
    try {
      return yamlMapper.readValue(yaml, AuditSettings.class);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("감사 설정 YAML 파싱 실패", e);
    }
  }

  /**
   * 세 개의 설정을 통합하여 PolicyToggleSettings를 생성합니다.
   *
   * @param authSettings 인증 설정
   * @param fileSettings 파일 설정
   * @param auditSettings 감사 설정
   * @return PolicyToggleSettings (기존 모듈 호환용)
   */
  public PolicyToggleSettings toPolicyToggleSettings(
      AuthenticationSettings authSettings,
      FileUploadSettings fileSettings,
      AuditSettings auditSettings) {

    return new PolicyToggleSettings(
        // Auth settings
        authSettings.passwordPolicyEnabled(),
        authSettings.passwordHistoryEnabled(),
        authSettings.accountLockEnabled(),
        authSettings.enabledLoginTypes(),
        // File settings
        fileSettings.maxFileSizeBytes(),
        fileSettings.allowedFileExtensions(),
        fileSettings.strictMimeValidation(),
        fileSettings.fileRetentionDays(),
        // Audit settings
        auditSettings.auditEnabled(),
        auditSettings.auditReasonRequired(),
        auditSettings.auditSensitiveApiDefaultOn(),
        auditSettings.auditRetentionDays(),
        auditSettings.auditStrictMode(),
        auditSettings.auditRiskLevel(),
        auditSettings.auditMaskingEnabled(),
        auditSettings.auditSensitiveEndpoints(),
        auditSettings.auditUnmaskRoles(),
        auditSettings.auditPartitionEnabled(),
        auditSettings.auditPartitionCron(),
        auditSettings.auditPartitionPreloadMonths(),
        auditSettings.auditMonthlyReportEnabled(),
        auditSettings.auditMonthlyReportCron(),
        auditSettings.auditLogRetentionEnabled(),
        auditSettings.auditLogRetentionCron(),
        auditSettings.auditColdArchiveEnabled(),
        auditSettings.auditColdArchiveCron(),
        auditSettings.auditRetentionCleanupEnabled(),
        auditSettings.auditRetentionCleanupCron(),
        BatchJobDefaults.defaults()
    );
  }

  /**
   * AuditSettings에서 AuditPartitionSettings를 추출합니다.
   *
   * @param auditSettings 감사 설정
   * @return AuditPartitionSettings
   */
  public AuditPartitionSettings toAuditPartitionSettings(AuditSettings auditSettings) {
    return new AuditPartitionSettings(
        auditSettings.auditPartitionEnabled(),
        auditSettings.auditPartitionCron(),
        auditSettings.auditPartitionPreloadMonths(),
        "", // tablespaceHot - 확장 설정에서 관리
        "", // tablespaceCold - 확장 설정에서 관리
        6,  // hotMonths - 기본값
        60  // coldMonths - 기본값
    );
  }

  /**
   * YAML에서 batchJobs 섹션을 파싱합니다.
   *
   * @param yaml YAML 문자열 (audit.settings)
   * @return BatchJob 스케줄 맵
   */
  public Map<BatchJobCode, BatchJobSchedule> parseBatchJobs(String yaml) {
    if (yaml == null || yaml.isBlank()) {
      return BatchJobDefaults.defaults();
    }
    try {
      JsonNode root = yamlMapper.readTree(yaml);
      JsonNode batchNode = root.get("batchJobs");
      if (batchNode == null || !batchNode.isObject()) {
        return BatchJobDefaults.defaults();
      }

      Map<BatchJobCode, BatchJobSchedule> result = new java.util.HashMap<>();
      var fields = batchNode.fields();
      while (fields.hasNext()) {
        var entry = fields.next();
        try {
          BatchJobCode code = BatchJobCode.valueOf(entry.getKey());
          BatchJobSchedule schedule = yamlMapper.treeToValue(entry.getValue(), BatchJobSchedule.class);
          result.put(code, schedule);
        } catch (IllegalArgumentException ignore) {
          // 알 수 없는 코드는 무시
        }
      }
      return result.isEmpty() ? BatchJobDefaults.defaults() : result;
    } catch (JsonProcessingException e) {
      return BatchJobDefaults.defaults();
    }
  }

  /**
   * Settings 객체를 YAML 문자열로 직렬화합니다.
   *
   * @param settings Settings 객체
   * @return YAML 문자열
   * @throws IllegalStateException 직렬화 실패 시
   */
  public String toYaml(Object settings) {
    try {
      return yamlMapper.writeValueAsString(settings);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("설정 YAML 직렬화 실패", e);
    }
  }
}
