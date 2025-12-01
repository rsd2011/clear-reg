package com.example.admin.systemconfig.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.admin.systemconfig.domain.SystemConfigRevision;
import com.example.admin.systemconfig.domain.SystemConfigRoot;
import com.example.admin.systemconfig.dto.settings.AuditSettings;
import com.example.admin.systemconfig.dto.settings.AuthenticationSettings;
import com.example.admin.systemconfig.dto.settings.FileUploadSettings;
import com.example.admin.systemconfig.event.SystemConfigChangedEvent;
import com.example.admin.systemconfig.repository.SystemConfigRootRepository;
import com.example.common.policy.AuditPartitionSettings;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;
import com.example.common.schedule.BatchJobCode;
import com.example.common.schedule.BatchJobDefaults;
import com.example.common.schedule.BatchJobSchedule;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import jakarta.annotation.PostConstruct;

/**
 * SystemConfig 기반 PolicySettingsProvider 구현체.
 * <p>
 * 세 개의 설정(auth.settings, file.settings, audit.settings)을 통합하여
 * 기존 PolicyToggleSettings 형식으로 제공합니다.
 * </p>
 * <p>
 * 캐시 메커니즘:
 * <ul>
 *   <li>AtomicReference 기반 인메모리 캐시</li>
 *   <li>설정 변경 시 자동 갱신 (이벤트 기반)</li>
 *   <li>캐시 미스 시 DB에서 로드</li>
 * </ul>
 * </p>
 * <p>
 * 활성화 조건:
 * <ul>
 *   <li>{@code clearreg.policy.provider=systemconfig} (기본값)</li>
 *   <li>{@code SystemConfigRootRepository} 빈이 존재해야 함</li>
 * </ul>
 * </p>
 */
@Component
@ConditionalOnProperty(
    name = "clearreg.policy.provider",
    havingValue = "systemconfig",
    matchIfMissing = true
)
@ConditionalOnBean(SystemConfigRootRepository.class)
public class SystemConfigPolicySettingsProvider implements PolicySettingsProvider {

  private static final Logger log = LoggerFactory.getLogger(SystemConfigPolicySettingsProvider.class);

  private final SystemConfigRootRepository rootRepository;
  private final SystemConfigSettingsParser parser;
  private final ApplicationEventPublisher eventPublisher;

  /** 캐시된 통합 설정 */
  private final AtomicReference<CachedSettings> cache = new AtomicReference<>();

  public SystemConfigPolicySettingsProvider(
      SystemConfigRootRepository rootRepository,
      SystemConfigSettingsParser parser,
      ApplicationEventPublisher eventPublisher) {
    this.rootRepository = rootRepository;
    this.parser = parser;
    this.eventPublisher = eventPublisher;
  }

  /**
   * 애플리케이션 시작 시 캐시 초기화.
   */
  @PostConstruct
  @Transactional(readOnly = true)
  public void initialize() {
    try {
      refreshCache();
      log.info("SystemConfig 설정 캐시가 초기화되었습니다.");
    } catch (Exception e) {
      log.warn("SystemConfig 설정 캐시 초기화 실패, 기본값 사용: {}", e.getMessage());
      cache.set(CachedSettings.defaults());
    }
  }

  @Override
  public PolicyToggleSettings currentSettings() {
    CachedSettings cached = cache.get();
    if (cached == null) {
      refreshCache();
      cached = cache.get();
    }
    return cached != null ? cached.policyToggleSettings() : createDefaultSettings();
  }

  @Override
  public AuditPartitionSettings partitionSettings() {
    CachedSettings cached = cache.get();
    if (cached == null) {
      refreshCache();
      cached = cache.get();
    }
    return cached != null ? cached.partitionSettings() : createDefaultPartitionSettings();
  }

  @Override
  public BatchJobSchedule batchJobSchedule(BatchJobCode code) {
    CachedSettings cached = cache.get();
    if (cached == null) {
      refreshCache();
      cached = cache.get();
    }
    if (cached != null && cached.batchJobs() != null) {
      return cached.batchJobs().get(code);
    }
    return BatchJobDefaults.defaults().get(code);
  }

  /**
   * 캐시를 강제로 갱신합니다.
   * 설정 변경 후 호출하면 최신 설정으로 업데이트됩니다.
   */
  @Transactional(readOnly = true)
  public void refreshCache() {
    AuthenticationSettings authSettings = loadSettings(
        SystemConfigSettingsParser.AUTH_CONFIG_CODE,
        AuthenticationSettings.defaults(),
        parser::parseAuthSettings
    );
    FileUploadSettings fileSettings = loadSettings(
        SystemConfigSettingsParser.FILE_CONFIG_CODE,
        FileUploadSettings.defaults(),
        parser::parseFileSettings
    );
    AuditSettings auditSettings = loadSettings(
        SystemConfigSettingsParser.AUDIT_CONFIG_CODE,
        AuditSettings.defaults(),
        parser::parseAuditSettings
    );

    PolicyToggleSettings toggleSettings = parser.toPolicyToggleSettings(
        authSettings, fileSettings, auditSettings
    );
    AuditPartitionSettings partitionSettings = parser.toAuditPartitionSettings(auditSettings);
    Map<BatchJobCode, BatchJobSchedule> batchJobs = loadBatchJobs();

    cache.set(new CachedSettings(
        authSettings,
        fileSettings,
        auditSettings,
        toggleSettings,
        partitionSettings,
        batchJobs
    ));
  }

  /**
   * 설정 변경을 알리고 캐시를 갱신합니다.
   *
   * @param configCode 변경된 설정 코드
   * @param newYaml 새 YAML 내용
   */
  public void notifySettingsChanged(String configCode, String newYaml) {
    refreshCache();
    if (eventPublisher != null) {
      eventPublisher.publishEvent(new SystemConfigChangedEvent(configCode, newYaml));
    }
    log.info("SystemConfig 설정이 변경되었습니다: {}", configCode);
  }

  /**
   * 현재 캐시된 개별 설정을 반환합니다.
   */
  public Optional<AuthenticationSettings> getAuthSettings() {
    CachedSettings cached = cache.get();
    return cached != null ? Optional.of(cached.authSettings()) : Optional.empty();
  }

  public Optional<FileUploadSettings> getFileSettings() {
    CachedSettings cached = cache.get();
    return cached != null ? Optional.of(cached.fileSettings()) : Optional.empty();
  }

  public Optional<AuditSettings> getAuditSettings() {
    CachedSettings cached = cache.get();
    return cached != null ? Optional.of(cached.auditSettings()) : Optional.empty();
  }

  // === Private Helper Methods ===

  @FunctionalInterface
  private interface SettingsParser<T> {
    T parse(String yaml);
  }

  private <T> T loadSettings(String configCode, T defaultValue, SettingsParser<T> parseFunction) {
    return rootRepository.findByConfigCode(configCode)
        .map(SystemConfigRoot::getCurrentVersion)
        .filter(revision -> revision != null && revision.isActive())
        .map(SystemConfigRevision::getYamlContent)
        .map(yaml -> {
          try {
            return parseFunction.parse(yaml);
          } catch (Exception e) {
            log.warn("설정 파싱 실패 ({}), 기본값 사용: {}", configCode, e.getMessage());
            return defaultValue;
          }
        })
        .orElse(defaultValue);
  }

  private Map<BatchJobCode, BatchJobSchedule> loadBatchJobs() {
    return rootRepository.findByConfigCode(SystemConfigSettingsParser.AUDIT_CONFIG_CODE)
        .map(SystemConfigRoot::getCurrentVersion)
        .filter(revision -> revision != null && revision.isActive())
        .map(SystemConfigRevision::getYamlContent)
        .map(parser::parseBatchJobs)
        .orElse(BatchJobDefaults.defaults());
  }

  private PolicyToggleSettings createDefaultSettings() {
    return parser.toPolicyToggleSettings(
        AuthenticationSettings.defaults(),
        FileUploadSettings.defaults(),
        AuditSettings.defaults()
    );
  }

  private AuditPartitionSettings createDefaultPartitionSettings() {
    return parser.toAuditPartitionSettings(AuditSettings.defaults());
  }

  /**
   * 캐시된 설정 값을 담는 내부 레코드.
   */
  private record CachedSettings(
      AuthenticationSettings authSettings,
      FileUploadSettings fileSettings,
      AuditSettings auditSettings,
      PolicyToggleSettings policyToggleSettings,
      AuditPartitionSettings partitionSettings,
      Map<BatchJobCode, BatchJobSchedule> batchJobs
  ) {
    static CachedSettings defaults() {
      AuthenticationSettings auth = AuthenticationSettings.defaults();
      FileUploadSettings file = FileUploadSettings.defaults();
      AuditSettings audit = AuditSettings.defaults();
      
      SystemConfigSettingsParser tempParser = null;
      PolicyToggleSettings toggle = new PolicyToggleSettings(
          auth.passwordPolicyEnabled(),
          auth.passwordHistoryEnabled(),
          auth.accountLockEnabled(),
          auth.enabledLoginTypes(),
          file.maxFileSizeBytes(),
          file.allowedFileExtensions(),
          file.strictMimeValidation(),
          file.fileRetentionDays(),
          audit.auditEnabled(),
          audit.auditReasonRequired(),
          audit.auditSensitiveApiDefaultOn(),
          audit.auditRetentionDays(),
          audit.auditStrictMode(),
          audit.auditRiskLevel(),
          audit.auditMaskingEnabled(),
          audit.auditSensitiveEndpoints(),
          audit.auditUnmaskRoles(),
          audit.auditPartitionEnabled(),
          audit.auditPartitionCron(),
          audit.auditPartitionPreloadMonths(),
          audit.auditMonthlyReportEnabled(),
          audit.auditMonthlyReportCron(),
          audit.auditLogRetentionEnabled(),
          audit.auditLogRetentionCron(),
          audit.auditColdArchiveEnabled(),
          audit.auditColdArchiveCron(),
          audit.auditRetentionCleanupEnabled(),
          audit.auditRetentionCleanupCron(),
          BatchJobDefaults.defaults()
      );
      AuditPartitionSettings partition = new AuditPartitionSettings(
          audit.auditPartitionEnabled(),
          audit.auditPartitionCron(),
          audit.auditPartitionPreloadMonths(),
          "", "", 6, 60
      );
      return new CachedSettings(auth, file, audit, toggle, partition, BatchJobDefaults.defaults());
    }
  }
}
