package com.example.admin.systemconfig.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.example.admin.systemconfig.domain.SystemConfigRevision;
import com.example.admin.systemconfig.domain.SystemConfigRoot;
import com.example.admin.systemconfig.dto.settings.AuditSettings;
import com.example.admin.systemconfig.dto.settings.AuthenticationSettings;
import com.example.admin.systemconfig.dto.settings.FileUploadSettings;
import com.example.admin.systemconfig.event.SystemConfigChangedEvent;
import com.example.admin.systemconfig.repository.SystemConfigRootRepository;
import com.example.common.policy.AuditPartitionSettings;
import com.example.common.policy.PolicyToggleSettings;
import com.example.common.schedule.BatchJobCode;
import com.example.common.schedule.BatchJobSchedule;
import com.example.common.version.ChangeAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@ExtendWith(MockitoExtension.class)
@DisplayName("SystemConfigPolicySettingsProvider 테스트")
class SystemConfigPolicySettingsProviderTest {

  @Mock
  private SystemConfigRootRepository rootRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  private SystemConfigSettingsParser parser;
  private SystemConfigPolicySettingsProvider provider;

  @BeforeEach
  void setUp() {
    ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();
    parser = new SystemConfigSettingsParser(yamlMapper);
    provider = new SystemConfigPolicySettingsProvider(rootRepository, parser, eventPublisher);
  }

  @Nested
  @DisplayName("currentSettings 테스트")
  class CurrentSettingsTest {

    @Test
    @DisplayName("Given: 캐시가 비어 있고 DB에 설정이 있을 때, When: currentSettings 호출, Then: DB에서 로드 후 반환")
    void shouldLoadFromDbWhenCacheEmpty() {
      // Given
      setupMockConfig("auth.settings", createAuthYaml());
      setupMockConfig("file.settings", createFileYaml());
      setupMockConfig("audit.settings", createAuditYaml());

      // When
      PolicyToggleSettings result = provider.currentSettings();

      // Then
      assertThat(result).isNotNull();
      assertThat(result.passwordPolicyEnabled()).isTrue();
      assertThat(result.maxFileSizeBytes()).isEqualTo(10485760L);
      assertThat(result.auditEnabled()).isTrue();
    }

    @Test
    @DisplayName("Given: DB에 설정이 없을 때, When: currentSettings 호출, Then: 기본값 반환")
    void shouldReturnDefaultsWhenNoConfig() {
      // Given
      when(rootRepository.findByConfigCode(any())).thenReturn(Optional.empty());

      // When
      PolicyToggleSettings result = provider.currentSettings();

      // Then
      assertThat(result).isNotNull();
      assertThat(result.passwordPolicyEnabled()).isTrue();  // 기본값
      assertThat(result.auditEnabled()).isTrue();  // 기본값
    }

    @Test
    @DisplayName("Given: 캐시가 이미 채워져 있을 때, When: currentSettings 두 번 호출, Then: DB 조회는 한 번만")
    void shouldUseCacheOnSecondCall() {
      // Given
      setupMockConfig("auth.settings", createAuthYaml());
      setupMockConfig("file.settings", createFileYaml());
      setupMockConfig("audit.settings", createAuditYaml());

      // When
      provider.currentSettings();  // 첫 번째 호출 - DB 조회
      provider.currentSettings();  // 두 번째 호출 - 캐시 사용

      // Then
      verify(rootRepository, times(1)).findByConfigCode("auth.settings");
    }
  }

  @Nested
  @DisplayName("partitionSettings 테스트")
  class PartitionSettingsTest {

    @Test
    @DisplayName("Given: audit.settings가 있을 때, When: partitionSettings 호출, Then: 파티션 설정 반환")
    void shouldReturnPartitionSettings() {
      // Given
      setupMockConfig("auth.settings", createAuthYaml());
      setupMockConfig("file.settings", createFileYaml());
      setupMockConfig("audit.settings", """
          auditEnabled: true
          auditPartitionEnabled: true
          auditPartitionCron: "0 0 1 * *"
          auditPartitionPreloadMonths: 6
          """);

      // When
      AuditPartitionSettings result = provider.partitionSettings();

      // Then
      assertThat(result).isNotNull();
      assertThat(result.enabled()).isTrue();
      assertThat(result.cron()).isEqualTo("0 0 1 * *");
      assertThat(result.preloadMonths()).isEqualTo(6);
    }
  }

  @Nested
  @DisplayName("batchJobSchedule 테스트")
  class BatchJobScheduleTest {

    @Test
    @DisplayName("Given: 설정이 없을 때, When: batchJobSchedule 호출, Then: 기본 스케줄 반환")
    void shouldReturnDefaultSchedule() {
      // Given
      when(rootRepository.findByConfigCode(any())).thenReturn(Optional.empty());

      // When
      BatchJobSchedule result = provider.batchJobSchedule(BatchJobCode.AUDIT_MONTHLY_REPORT);

      // Then
      assertThat(result).isNotNull();
    }
  }

  @Nested
  @DisplayName("refreshCache 테스트")
  class RefreshCacheTest {

    @Test
    @DisplayName("Given: DB에 새 설정이 있을 때, When: refreshCache 호출, Then: 캐시 갱신")
    void shouldRefreshCache() {
      // Given
      setupMockConfig("auth.settings", createAuthYaml());
      setupMockConfig("file.settings", createFileYaml());
      setupMockConfig("audit.settings", createAuditYaml());

      // When
      provider.refreshCache();

      // Then - 캐시된 개별 설정 확인
      assertThat(provider.getAuthSettings()).isPresent();
      assertThat(provider.getFileSettings()).isPresent();
      assertThat(provider.getAuditSettings()).isPresent();
    }
  }

  @Nested
  @DisplayName("notifySettingsChanged 테스트")
  class NotifySettingsChangedTest {

    @Test
    @DisplayName("Given: 설정 변경, When: notifySettingsChanged 호출, Then: 캐시 갱신 및 이벤트 발행")
    void shouldRefreshCacheAndPublishEvent() {
      // Given
      setupMockConfig("auth.settings", createAuthYaml());
      setupMockConfig("file.settings", createFileYaml());
      setupMockConfig("audit.settings", createAuditYaml());

      // When
      provider.notifySettingsChanged("auth.settings", createAuthYaml());

      // Then
      ArgumentCaptor<SystemConfigChangedEvent> captor =
          ArgumentCaptor.forClass(SystemConfigChangedEvent.class);
      verify(eventPublisher).publishEvent(captor.capture());

      SystemConfigChangedEvent event = captor.getValue();
      assertThat(event.getConfigCode()).isEqualTo("auth.settings");
    }
  }

  @Nested
  @DisplayName("개별 설정 조회 테스트")
  class IndividualSettingsTest {

    @Test
    @DisplayName("Given: 캐시가 비어 있을 때, When: getAuthSettings 호출, Then: empty 반환")
    void shouldReturnEmptyWhenCacheEmpty() {
      // When
      Optional<AuthenticationSettings> result = provider.getAuthSettings();

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Given: 캐시가 채워져 있을 때, When: getAuthSettings 호출, Then: 설정 반환")
    void shouldReturnSettingsWhenCached() {
      // Given
      setupMockConfig("auth.settings", createAuthYaml());
      setupMockConfig("file.settings", createFileYaml());
      setupMockConfig("audit.settings", createAuditYaml());
      provider.refreshCache();

      // When
      Optional<AuthenticationSettings> result = provider.getAuthSettings();

      // Then
      assertThat(result).isPresent();
      assertThat(result.get().passwordPolicyEnabled()).isTrue();
    }

    @Test
    @DisplayName("Given: 캐시가 채워져 있을 때, When: getFileSettings 호출, Then: 설정 반환")
    void shouldReturnFileSettings() {
      // Given
      setupMockConfig("auth.settings", createAuthYaml());
      setupMockConfig("file.settings", createFileYaml());
      setupMockConfig("audit.settings", createAuditYaml());
      provider.refreshCache();

      // When
      Optional<FileUploadSettings> result = provider.getFileSettings();

      // Then
      assertThat(result).isPresent();
      assertThat(result.get().maxFileSizeBytes()).isEqualTo(10485760L);
    }

    @Test
    @DisplayName("Given: 캐시가 채워져 있을 때, When: getAuditSettings 호출, Then: 설정 반환")
    void shouldReturnAuditSettings() {
      // Given
      setupMockConfig("auth.settings", createAuthYaml());
      setupMockConfig("file.settings", createFileYaml());
      setupMockConfig("audit.settings", createAuditYaml());
      provider.refreshCache();

      // When
      Optional<AuditSettings> result = provider.getAuditSettings();

      // Then
      assertThat(result).isPresent();
      assertThat(result.get().auditEnabled()).isTrue();
    }
  }

  @Nested
  @DisplayName("initialize 테스트")
  class InitializeTest {

    @Test
    @DisplayName("Given: DB에 설정이 있을 때, When: initialize 호출, Then: 캐시가 채워짐")
    void shouldInitializeCache() {
      // Given
      setupMockConfig("auth.settings", createAuthYaml());
      setupMockConfig("file.settings", createFileYaml());
      setupMockConfig("audit.settings", createAuditYaml());

      // When
      provider.initialize();

      // Then
      assertThat(provider.getAuthSettings()).isPresent();
      assertThat(provider.getFileSettings()).isPresent();
      assertThat(provider.getAuditSettings()).isPresent();
    }

    @Test
    @DisplayName("Given: DB 오류 발생 시, When: initialize 호출, Then: 기본값으로 캐시 초기화")
    void shouldUsDefaultsOnDbError() {
      // Given
      when(rootRepository.findByConfigCode(any())).thenThrow(new RuntimeException("DB error"));

      // When
      provider.initialize();

      // Then - currentSettings는 기본값을 반환해야 함
      PolicyToggleSettings result = provider.currentSettings();
      assertThat(result).isNotNull();
      assertThat(result.passwordPolicyEnabled()).isTrue(); // default value
    }

    @Test
    @DisplayName("Given: DB에 설정이 없을 때, When: initialize 호출, Then: 기본값 사용")
    void shouldUsDefaultsWhenNoConfig() {
      // Given
      when(rootRepository.findByConfigCode(any())).thenReturn(Optional.empty());

      // When
      provider.initialize();

      // Then
      assertThat(provider.currentSettings()).isNotNull();
      assertThat(provider.partitionSettings()).isNotNull();
    }
  }

  @Nested
  @DisplayName("batchJobSchedule 추가 테스트")
  class BatchJobScheduleAdditionalTest {

    @Test
    @DisplayName("Given: 캐시가 채워져 있을 때, When: batchJobSchedule 호출, Then: 스케줄 반환")
    void shouldReturnScheduleFromCache() {
      // Given
      setupMockConfig("auth.settings", createAuthYaml());
      setupMockConfig("file.settings", createFileYaml());
      setupMockConfig("audit.settings", createAuditYaml());
      provider.refreshCache();

      // When
      BatchJobSchedule result = provider.batchJobSchedule(BatchJobCode.AUDIT_MONTHLY_REPORT);

      // Then
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Given: 캐시에 batchJobs가 없을 때, When: batchJobSchedule 호출, Then: 기본 스케줄 반환")
    void shouldReturnDefaultScheduleWhenNoBatchJobsInCache() {
      // Given - 비활성 revision으로 설정 (batchJobs가 null이 되도록)
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("audit.settings", "audit name", null, now);
      SystemConfigRevision revision = SystemConfigRevision.create(
          root, 1, createAuditYaml(), false, ChangeAction.CREATE, null,
          "system", "system", now);
      root.activateNewVersion(revision, now);

      when(rootRepository.findByConfigCode("audit.settings")).thenReturn(Optional.of(root));
      when(rootRepository.findByConfigCode("auth.settings")).thenReturn(Optional.empty());
      when(rootRepository.findByConfigCode("file.settings")).thenReturn(Optional.empty());
      provider.refreshCache();

      // When
      BatchJobSchedule result = provider.batchJobSchedule(BatchJobCode.AUDIT_MONTHLY_REPORT);

      // Then
      assertThat(result).isNotNull();
    }
  }

  @Nested
  @DisplayName("파싱 실패 케이스 테스트")
  class ParsingFailureTest {

    @Test
    @DisplayName("Given: 잘못된 YAML 포맷, When: refreshCache 호출, Then: 기본값 사용")
    void shouldUseDefaultsOnInvalidYaml() {
      // Given
      setupMockConfig("auth.settings", "invalid: yaml: content: {{{{");
      setupMockConfig("file.settings", createFileYaml());
      setupMockConfig("audit.settings", createAuditYaml());

      // When
      provider.refreshCache();

      // Then - 파싱 실패해도 기본값으로 동작
      PolicyToggleSettings result = provider.currentSettings();
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Given: 비활성 버전만 존재, When: currentSettings 호출, Then: 기본값 반환")
    void shouldReturnDefaultsWhenOnlyInactiveVersion() {
      // Given
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("auth.settings", "auth name", null, now);
      SystemConfigRevision inactiveRevision = SystemConfigRevision.create(
          root, 1, createAuthYaml(), false, ChangeAction.CREATE, null,
          "system", "system", now);
      root.activateNewVersion(inactiveRevision, now);

      when(rootRepository.findByConfigCode("auth.settings")).thenReturn(Optional.of(root));
      when(rootRepository.findByConfigCode("file.settings")).thenReturn(Optional.empty());
      when(rootRepository.findByConfigCode("audit.settings")).thenReturn(Optional.empty());

      // When
      PolicyToggleSettings result = provider.currentSettings();

      // Then
      assertThat(result).isNotNull();
      assertThat(result.passwordPolicyEnabled()).isTrue(); // 기본값
    }
  }

  @Nested
  @DisplayName("notifySettingsChanged eventPublisher null 케이스 테스트")
  class NotifySettingsChangedNullPublisherTest {

    @Test
    @DisplayName("Given: eventPublisher가 null일 때, When: notifySettingsChanged 호출, Then: 예외 없이 완료")
    void shouldHandleNullEventPublisher() {
      // Given - eventPublisher가 null인 provider 생성
      SystemConfigPolicySettingsProvider providerWithNullPublisher =
          new SystemConfigPolicySettingsProvider(rootRepository, parser, null);

      setupMockConfig("auth.settings", createAuthYaml());
      setupMockConfig("file.settings", createFileYaml());
      setupMockConfig("audit.settings", createAuditYaml());

      // When & Then - 예외 없이 완료되어야 함
      providerWithNullPublisher.notifySettingsChanged("auth.settings", createAuthYaml());
    }
  }

  @Nested
  @DisplayName("getFileSettings 및 getAuditSettings 캐시 미스 테스트")
  class CacheMissTest {

    @Test
    @DisplayName("Given: 캐시가 비어 있을 때, When: getFileSettings 호출, Then: empty 반환")
    void shouldReturnEmptyForFileSettingsWhenCacheEmpty() {
      // When
      Optional<FileUploadSettings> result = provider.getFileSettings();

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Given: 캐시가 비어 있을 때, When: getAuditSettings 호출, Then: empty 반환")
    void shouldReturnEmptyForAuditSettingsWhenCacheEmpty() {
      // When
      Optional<AuditSettings> result = provider.getAuditSettings();

      // Then
      assertThat(result).isEmpty();
    }
  }

  // === Helper Methods ===

  private void setupMockConfig(String configCode, String yaml) {
    OffsetDateTime now = OffsetDateTime.now();
    SystemConfigRoot root = SystemConfigRoot.create(configCode, configCode + " name", null, now);
    SystemConfigRevision revision = SystemConfigRevision.create(
        root, 1, yaml, true, ChangeAction.CREATE, null,
        "system", "system", now);
    root.activateNewVersion(revision, now);

    when(rootRepository.findByConfigCode(configCode)).thenReturn(Optional.of(root));
  }

  private String createAuthYaml() {
    return """
        passwordPolicyEnabled: true
        passwordHistoryEnabled: true
        passwordHistoryCount: 5
        accountLockEnabled: true
        accountLockThreshold: 5
        accountLockDurationMinutes: 30
        enabledLoginTypes:
          - PASSWORD
        sessionTimeoutMinutes: 30
        concurrentSessionAllowed: false
        maxConcurrentSessions: 1
        ssoEnabled: false
        """;
  }

  private String createFileYaml() {
    return """
        maxFileSizeBytes: 10485760
        allowedFileExtensions:
          - pdf
          - docx
          - xlsx
        strictMimeValidation: true
        fileRetentionDays: 90
        """;
  }

  private String createAuditYaml() {
    return """
        auditEnabled: true
        auditReasonRequired: false
        auditSensitiveApiDefaultOn: true
        auditRetentionDays: 90
        auditStrictMode: false
        auditRiskLevel: 2
        auditMaskingEnabled: true
        auditPartitionEnabled: true
        auditPartitionCron: "0 0 1 * *"
        auditPartitionPreloadMonths: 3
        """;
  }
}
