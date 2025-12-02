package com.example.admin.systemconfig.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.admin.systemconfig.dto.settings.AuditSettings;
import com.example.admin.systemconfig.dto.settings.AuthenticationSettings;
import com.example.admin.systemconfig.dto.settings.FileUploadSettings;
import com.example.common.policy.PolicyToggleSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@DisplayName("SystemConfigSettingsParser 테스트")
class SystemConfigSettingsParserTest {

  private SystemConfigSettingsParser parser;

  @BeforeEach
  void setUp() {
    ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory())
        .findAndRegisterModules();
    parser = new SystemConfigSettingsParser(yamlMapper);
  }

  @Nested
  @DisplayName("parseAuthSettings 테스트")
  class ParseAuthSettingsTest {

    @Test
    @DisplayName("Given: 유효한 YAML, When: parseAuthSettings 호출, Then: AuthenticationSettings 반환")
    void shouldParseValidYaml() {
      // Given
      String yaml = """
          passwordPolicyEnabled: true
          passwordHistoryEnabled: false
          passwordHistoryCount: 10
          accountLockEnabled: true
          accountLockThreshold: 3
          accountLockDurationMinutes: 60
          enabledLoginTypes:
            - PASSWORD
            - SSO
          sessionTimeoutMinutes: 120
          concurrentSessionAllowed: true
          maxConcurrentSessions: 5
          ssoEnabled: true
          ssoProvider: LDAP
          """;

      // When
      AuthenticationSettings result = parser.parseAuthSettings(yaml);

      // Then
      assertThat(result.passwordPolicyEnabled()).isTrue();
      assertThat(result.passwordHistoryEnabled()).isFalse();
      assertThat(result.passwordHistoryCount()).isEqualTo(10);
      assertThat(result.accountLockThreshold()).isEqualTo(3);
      assertThat(result.enabledLoginTypes()).containsExactly("PASSWORD", "SSO");
      assertThat(result.sessionTimeoutMinutes()).isEqualTo(120);
      assertThat(result.ssoEnabled()).isTrue();
      assertThat(result.ssoProvider()).isEqualTo("LDAP");
    }

    @Test
    @DisplayName("Given: null YAML, When: parseAuthSettings 호출, Then: 기본값 반환")
    void shouldReturnDefaultsForNull() {
      // When
      AuthenticationSettings result = parser.parseAuthSettings(null);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.passwordPolicyEnabled()).isTrue();
      assertThat(result.enabledLoginTypes()).containsExactly("PASSWORD");
    }

    @Test
    @DisplayName("Given: 빈 YAML, When: parseAuthSettings 호출, Then: 기본값 반환")
    void shouldReturnDefaultsForBlank() {
      // When
      AuthenticationSettings result = parser.parseAuthSettings("   ");

      // Then
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Given: 잘못된 YAML, When: parseAuthSettings 호출, Then: 예외 발생")
    void shouldThrowExceptionForInvalidYaml() {
      // Given
      String invalidYaml = "invalid: [\nbroken yaml";

      // When/Then
      assertThatThrownBy(() -> parser.parseAuthSettings(invalidYaml))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("인증 설정 YAML 파싱 실패");
    }
  }

  @Nested
  @DisplayName("parseFileSettings 테스트")
  class ParseFileSettingsTest {

    @Test
    @DisplayName("Given: 유효한 YAML, When: parseFileSettings 호출, Then: FileUploadSettings 반환")
    void shouldParseValidYaml() {
      // Given
      String yaml = """
          maxFileSizeBytes: 52428800
          allowedFileExtensions:
            - pdf
            - docx
            - xlsx
          strictMimeValidation: true
          fileRetentionDays: 365
          """;

      // When
      FileUploadSettings result = parser.parseFileSettings(yaml);

      // Then
      assertThat(result.maxFileSizeBytes()).isEqualTo(52428800L);
      assertThat(result.allowedFileExtensions()).containsExactly("pdf", "docx", "xlsx");
      assertThat(result.strictMimeValidation()).isTrue();
      assertThat(result.fileRetentionDays()).isEqualTo(365);
    }

    @Test
    @DisplayName("Given: null YAML, When: parseFileSettings 호출, Then: 기본값 반환")
    void shouldReturnDefaultsForNull() {
      // When
      FileUploadSettings result = parser.parseFileSettings(null);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.maxFileSizeBytes()).isGreaterThan(0);
    }
  }

  @Nested
  @DisplayName("parseAuditSettings 테스트")
  class ParseAuditSettingsTest {

    @Test
    @DisplayName("Given: 유효한 YAML, When: parseAuditSettings 호출, Then: AuditSettings 반환")
    void shouldParseValidYaml() {
      // Given
      String yaml = """
          auditEnabled: true
          auditReasonRequired: true
          auditSensitiveApiDefaultOn: false
          auditRetentionDays: 90
          auditStrictMode: true
          auditRiskLevel: 3
          auditMaskingEnabled: true
          auditUnmaskRoles:
            - ADMIN
            - AUDITOR
          auditPartitionEnabled: true
          auditPartitionCron: "0 0 1 * *"
          auditPartitionPreloadMonths: 3
          """;

      // When
      AuditSettings result = parser.parseAuditSettings(yaml);

      // Then
      assertThat(result.auditEnabled()).isTrue();
      assertThat(result.auditReasonRequired()).isTrue();
      assertThat(result.auditRetentionDays()).isEqualTo(90);
      assertThat(result.auditStrictMode()).isTrue();
      assertThat(result.auditRiskLevel()).isEqualTo("3");
      assertThat(result.auditMaskingEnabled()).isTrue();
      assertThat(result.auditUnmaskRoles()).containsExactly("ADMIN", "AUDITOR");
    }

    @Test
    @DisplayName("Given: null YAML, When: parseAuditSettings 호출, Then: 기본값 반환")
    void shouldReturnDefaultsForNull() {
      // When
      AuditSettings result = parser.parseAuditSettings(null);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.auditEnabled()).isTrue();
    }
  }

  @Nested
  @DisplayName("toPolicyToggleSettings 테스트")
  class ToPolicyToggleSettingsTest {

    @Test
    @DisplayName("Given: 세 개의 설정, When: toPolicyToggleSettings 호출, Then: 통합된 PolicyToggleSettings 반환")
    void shouldCombineSettings() {
      // Given
      AuthenticationSettings authSettings = new AuthenticationSettings(
          true, true, 5, true, 5, 30,
          List.of("PASSWORD", "SSO"), 30, false, 1, true, "LDAP"
      );
      FileUploadSettings fileSettings = FileUploadSettings.defaults();
      AuditSettings auditSettings = AuditSettings.defaults();

      // When
      PolicyToggleSettings result = parser.toPolicyToggleSettings(
          authSettings, fileSettings, auditSettings);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.passwordPolicyEnabled()).isTrue();
      assertThat(result.enabledLoginTypes()).containsExactly("PASSWORD", "SSO");
      assertThat(result.maxFileSizeBytes()).isEqualTo(fileSettings.maxFileSizeBytes());
      assertThat(result.allowedFileExtensions()).isEqualTo(fileSettings.allowedFileExtensions());
      assertThat(result.auditEnabled()).isTrue();
    }
  }

  @Nested
  @DisplayName("toYaml 테스트")
  class ToYamlTest {

    @Test
    @DisplayName("Given: Settings 객체, When: toYaml 호출, Then: YAML 문자열 반환")
    void shouldSerializeToYaml() {
      // Given
      AuthenticationSettings settings = AuthenticationSettings.defaults();

      // When
      String yaml = parser.toYaml(settings);

      // Then
      assertThat(yaml).isNotBlank();
      assertThat(yaml).contains("passwordPolicyEnabled");
      assertThat(yaml).contains("enabledLoginTypes");
    }
  }

  @Nested
  @DisplayName("상수 테스트")
  class ConstantsTest {

    @Test
    @DisplayName("설정 코드 상수가 올바르게 정의됨")
    void shouldHaveCorrectConfigCodes() {
      assertThat(SystemConfigSettingsParser.AUTH_CONFIG_CODE).isEqualTo("auth.settings");
      assertThat(SystemConfigSettingsParser.FILE_CONFIG_CODE).isEqualTo("file.settings");
      assertThat(SystemConfigSettingsParser.AUDIT_CONFIG_CODE).isEqualTo("audit.settings");
    }
  }

  @Nested
  @DisplayName("toAuditPartitionSettings 테스트")
  class ToAuditPartitionSettingsTest {

    @Test
    @DisplayName("Given: AuditSettings, When: toAuditPartitionSettings 호출, Then: AuditPartitionSettings 반환")
    void shouldConvertToPartitionSettings() {
      // Given
      String yaml = """
          auditEnabled: true
          auditPartitionEnabled: true
          auditPartitionCron: "0 0 1 * *"
          auditPartitionPreloadMonths: 6
          """;
      AuditSettings auditSettings = parser.parseAuditSettings(yaml);

      // When
      var result = parser.toAuditPartitionSettings(auditSettings);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.enabled()).isTrue();
      assertThat(result.cron()).isEqualTo("0 0 1 * *");
      assertThat(result.preloadMonths()).isEqualTo(6);
    }
  }

  @Nested
  @DisplayName("parseBatchJobs 테스트")
  class ParseBatchJobsTest {

    @Test
    @DisplayName("Given: null YAML, When: parseBatchJobs 호출, Then: 기본값 반환")
    void shouldReturnDefaultsForNull() {
      // When
      var result = parser.parseBatchJobs(null);

      // Then
      assertThat(result).isNotNull();
      assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Given: 빈 YAML, When: parseBatchJobs 호출, Then: 기본값 반환")
    void shouldReturnDefaultsForBlank() {
      // When
      var result = parser.parseBatchJobs("   ");

      // Then
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Given: batchJobs가 없는 YAML, When: parseBatchJobs 호출, Then: 기본값 반환")
    void shouldReturnDefaultsWhenNoBatchJobs() {
      // Given
      String yaml = """
          auditEnabled: true
          auditRetentionDays: 90
          """;

      // When
      var result = parser.parseBatchJobs(yaml);

      // Then
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Given: batchJobs가 있는 YAML, When: parseBatchJobs 호출, Then: 파싱된 결과 반환")
    void shouldParseBatchJobs() {
      // Given
      String yaml = """
          auditEnabled: true
          batchJobs:
            AUDIT_MONTHLY_REPORT:
              cron: "0 0 1 * * ?"
              enabled: true
            UNKNOWN_JOB:
              cron: "invalid"
              enabled: false
          """;

      // When
      var result = parser.parseBatchJobs(yaml);

      // Then
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Given: batchJobs가 비객체 타입일 때, When: parseBatchJobs 호출, Then: 기본값 반환")
    void shouldReturnDefaultsWhenBatchJobsNotObject() {
      // Given
      String yaml = """
          batchJobs: "not an object"
          """;

      // When
      var result = parser.parseBatchJobs(yaml);

      // Then
      assertThat(result).isNotNull();
    }
  }

  @Nested
  @DisplayName("parseFileSettings 추가 테스트")
  class ParseFileSettingsAdditionalTest {

    @Test
    @DisplayName("Given: 잘못된 YAML, When: parseFileSettings 호출, Then: 예외 발생")
    void shouldThrowExceptionForInvalidYaml() {
      // Given
      String invalidYaml = "invalid: [\nbroken";

      // When/Then
      assertThatThrownBy(() -> parser.parseFileSettings(invalidYaml))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("파일 설정 YAML 파싱 실패");
    }
  }

  @Nested
  @DisplayName("parseAuditSettings 추가 테스트")
  class ParseAuditSettingsAdditionalTest {

    @Test
    @DisplayName("Given: 잘못된 YAML, When: parseAuditSettings 호출, Then: 예외 발생")
    void shouldThrowExceptionForInvalidYaml() {
      // Given
      String invalidYaml = "invalid: [\nbroken";

      // When/Then
      assertThatThrownBy(() -> parser.parseAuditSettings(invalidYaml))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("감사 설정 YAML 파싱 실패");
    }
  }

  @Nested
  @DisplayName("parseBatchJobs 추가 테스트")
  class ParseBatchJobsAdditionalTest {

    @Test
    @DisplayName("Given: 잘못된 YAML 형식, When: parseBatchJobs 호출, Then: 기본값 반환")
    void shouldReturnDefaultsForInvalidYaml() {
      // Given - JsonProcessingException을 발생시키는 잘못된 YAML
      String invalidYaml = "invalid: [\nbroken";

      // When
      var result = parser.parseBatchJobs(invalidYaml);

      // Then - 예외 없이 기본값 반환
      assertThat(result).isNotNull();
      assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Given: 유효한 BatchJob 코드가 포함된 YAML, When: parseBatchJobs 호출, Then: 파싱된 스케줄 반환")
    void shouldParseValidBatchJobCodes() {
      // Given
      String yaml = """
          batchJobs:
            AUDIT_MONTHLY_REPORT:
              cron: "0 0 1 * * ?"
              enabled: true
              description: "월간 감사 보고서"
          """;

      // When
      var result = parser.parseBatchJobs(yaml);

      // Then
      assertThat(result).isNotNull();
      assertThat(result).containsKey(com.example.common.schedule.BatchJobCode.AUDIT_MONTHLY_REPORT);
    }

    @Test
    @DisplayName("Given: 알 수 없는 BatchJob 코드만 있는 YAML, When: parseBatchJobs 호출, Then: 기본값 반환")
    void shouldReturnDefaultsWhenOnlyUnknownBatchJobCodes() {
      // Given - 유효하지 않은 BatchJobCode만 포함
      String yaml = """
          batchJobs:
            UNKNOWN_JOB_CODE_1:
              cron: "0 0 * * * ?"
              enabled: true
            ANOTHER_UNKNOWN_CODE:
              cron: "0 30 * * * ?"
              enabled: false
          """;

      // When
      var result = parser.parseBatchJobs(yaml);

      // Then - 알 수 없는 코드는 무시되고 결과가 비어있으므로 기본값 반환
      assertThat(result).isNotNull();
      assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Given: 혼합된 유효/무효 BatchJob 코드, When: parseBatchJobs 호출, Then: 유효한 것만 파싱")
    void shouldParseOnlyValidBatchJobCodes() {
      // Given
      String yaml = """
          batchJobs:
            AUDIT_MONTHLY_REPORT:
              expression: "0 0 1 * * ?"
              enabled: true
              triggerType: CRON
            INVALID_CODE:
              expression: "0 0 * * * ?"
              enabled: false
            AUDIT_LOG_RETENTION:
              expression: "0 0 2 * * ?"
              enabled: true
              triggerType: CRON
          """;

      // When
      var result = parser.parseBatchJobs(yaml);

      // Then
      assertThat(result).isNotNull();
      assertThat(result).containsKey(com.example.common.schedule.BatchJobCode.AUDIT_MONTHLY_REPORT);
      assertThat(result).containsKey(com.example.common.schedule.BatchJobCode.AUDIT_LOG_RETENTION);
      // 유효한 코드만 파싱됨 (INVALID_CODE는 무시됨)
      // 파싱된 스케줄의 expression 값 검증
      assertThat(result.get(com.example.common.schedule.BatchJobCode.AUDIT_MONTHLY_REPORT).expression())
          .isEqualTo("0 0 1 * * ?");
    }
  }

  @Nested
  @DisplayName("parseFileSettings 빈 YAML 테스트")
  class ParseFileSettingsBlankTest {

    @Test
    @DisplayName("Given: 빈 YAML, When: parseFileSettings 호출, Then: 기본값 반환")
    void shouldReturnDefaultsForBlank() {
      // When
      FileUploadSettings result = parser.parseFileSettings("   ");

      // Then
      assertThat(result).isNotNull();
      assertThat(result.maxFileSizeBytes()).isGreaterThan(0);
    }
  }

  @Nested
  @DisplayName("parseAuditSettings 빈 YAML 테스트")
  class ParseAuditSettingsBlankTest {

    @Test
    @DisplayName("Given: 빈 YAML, When: parseAuditSettings 호출, Then: 기본값 반환")
    void shouldReturnDefaultsForBlank() {
      // When
      AuditSettings result = parser.parseAuditSettings("   ");

      // Then
      assertThat(result).isNotNull();
      assertThat(result.auditEnabled()).isTrue();
    }
  }
}
