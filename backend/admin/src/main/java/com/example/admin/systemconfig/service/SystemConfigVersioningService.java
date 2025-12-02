package com.example.admin.systemconfig.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.admin.permission.context.AuthContext;
import com.example.admin.systemconfig.domain.SystemConfigRevision;
import com.example.admin.systemconfig.domain.SystemConfigRoot;
import com.example.admin.systemconfig.dto.SystemConfigCompareResponse;
import com.example.admin.systemconfig.dto.SystemConfigDraftRequest;
import com.example.admin.systemconfig.dto.SystemConfigRevisionResponse;
import com.example.admin.systemconfig.dto.SystemConfigRootRequest;
import com.example.admin.systemconfig.dto.SystemConfigRootResponse;
import com.example.admin.systemconfig.exception.SystemConfigNotFoundException;
import com.example.admin.systemconfig.repository.SystemConfigRevisionRepository;
import com.example.admin.systemconfig.repository.SystemConfigRootRepository;
import com.example.common.version.ChangeAction;

/**
 * 시스템 설정 버전 관리 서비스 (SCD Type 2).
 * <p>
 * 활성화 조건: SystemConfigRootRepository 빈이 존재해야 함
 * </p>
 */
@Service
@Transactional
@ConditionalOnBean(SystemConfigRootRepository.class)
public class SystemConfigVersioningService {

  private final SystemConfigRootRepository rootRepository;
  private final SystemConfigRevisionRepository revisionRepository;
  private final org.springframework.beans.factory.ObjectProvider<SystemConfigPolicySettingsProvider> settingsProviderProvider;

  public SystemConfigVersioningService(SystemConfigRootRepository rootRepository,
      SystemConfigRevisionRepository revisionRepository,
      org.springframework.beans.factory.ObjectProvider<SystemConfigPolicySettingsProvider> settingsProviderProvider) {
    this.rootRepository = rootRepository;
    this.revisionRepository = revisionRepository;
    this.settingsProviderProvider = settingsProviderProvider;
  }

  /**
   * 설정 변경 알림 (캐시 갱신).
   */
  private void notifySettingsChanged(String configCode, String yaml) {
    SystemConfigPolicySettingsProvider provider = settingsProviderProvider.getIfAvailable();
    if (provider != null) {
      provider.notifySettingsChanged(configCode, yaml);
    }
  }

  // ==========================================================================
  // 설정 루트 관리
  // ==========================================================================

  /**
   * 모든 시스템 설정 목록 조회.
   */
  @Transactional(readOnly = true)
  public List<SystemConfigRootResponse> getAllConfigs() {
    return rootRepository.findAll().stream()
        .map(SystemConfigRootResponse::from)
        .toList();
  }

  /**
   * 설정 코드로 루트 조회.
   */
  @Transactional(readOnly = true)
  public SystemConfigRootResponse getConfigByCode(String configCode) {
    SystemConfigRoot root = rootRepository.findByConfigCode(configCode)
        .orElseThrow(() -> new SystemConfigNotFoundException("설정을 찾을 수 없습니다: " + configCode));
    return SystemConfigRootResponse.from(root);
  }

  /**
   * 설정 코드로 루트 존재 여부 확인.
   *
   * @param configCode 설정 코드
   * @return 루트 응답 Optional
   */
  @Transactional(readOnly = true)
  public java.util.Optional<SystemConfigRootResponse> findByConfigCode(String configCode) {
    return rootRepository.findByConfigCode(configCode)
        .map(SystemConfigRootResponse::from);
  }

  /**
   * 새 시스템 설정 생성.
   */
  public SystemConfigRootResponse createConfig(SystemConfigRootRequest request, AuthContext context) {
    if (rootRepository.existsByConfigCode(request.configCode())) {
      throw new IllegalArgumentException("이미 존재하는 설정 코드입니다: " + request.configCode());
    }

    OffsetDateTime now = OffsetDateTime.now();

    // 루트 생성
    SystemConfigRoot root = SystemConfigRoot.create(
        request.configCode(),
        request.name(),
        request.description(),
        now
    );
    root = rootRepository.save(root);

    // 첫 번째 버전 생성
    SystemConfigRevision revision = SystemConfigRevision.create(
        root,
        1,
        request.yamlContent(),
        request.active(),
        ChangeAction.CREATE,
        null,
        context.username(),
        context.username(),
        now
    );
    revision = revisionRepository.save(revision);
    root.activateNewVersion(revision, now);

    // 설정 변경 알림
    notifySettingsChanged(request.configCode(), request.yamlContent());

    return SystemConfigRootResponse.from(root);
  }

  /**
   * 시스템 설정 직접 수정 (새 버전 생성).
   */
  public SystemConfigRevisionResponse updateConfig(UUID configId,
      SystemConfigDraftRequest request,
      AuthContext context) {
    SystemConfigRoot root = findRootOrThrow(configId);
    OffsetDateTime now = OffsetDateTime.now();

    // 현재 버전 종료
    SystemConfigRevision currentVersion = root.getCurrentVersion();
    if (currentVersion != null) {
      currentVersion.close(now);
    }

    // 새 버전 생성
    int nextVersionNumber = revisionRepository.findMaxVersionByRootId(configId) + 1;
    SystemConfigRevision newVersion = SystemConfigRevision.create(
        root,
        nextVersionNumber,
        request.yamlContent(),
        request.active(),
        ChangeAction.UPDATE,
        request.changeReason(),
        context.username(),
        context.username(),
        now
    );

    newVersion = revisionRepository.save(newVersion);
    root.activateNewVersion(newVersion, now);

    // 설정 변경 알림
    notifySettingsChanged(root.getConfigCode(), request.yamlContent());

    return SystemConfigRevisionResponse.from(newVersion);
  }

  /**
   * 시스템 설정 삭제 (비활성화).
   */
  public SystemConfigRevisionResponse deleteConfig(UUID configId, AuthContext context) {
    SystemConfigRoot root = findRootOrThrow(configId);
    OffsetDateTime now = OffsetDateTime.now();

    SystemConfigRevision currentVersion = root.getCurrentVersion();
    if (currentVersion != null) {
      currentVersion.close(now);
    }

    int nextVersionNumber = revisionRepository.findMaxVersionByRootId(configId) + 1;
    SystemConfigRevision deleteVersion = SystemConfigRevision.create(
        root,
        nextVersionNumber,
        currentVersion != null ? currentVersion.getYamlContent() : "",
        false, // 비활성화
        ChangeAction.DELETE,
        null,
        context.username(),
        context.username(),
        now
    );

    deleteVersion = revisionRepository.save(deleteVersion);
    root.activateNewVersion(deleteVersion, now);

    // 설정 삭제 알림 (빈 내용 전달)
    notifySettingsChanged(root.getConfigCode(), null);

    return SystemConfigRevisionResponse.from(deleteVersion);
  }

  // ==========================================================================
  // 버전 이력 조회
  // ==========================================================================

  /**
   * 버전 이력 목록 조회 (최신순, Draft 제외).
   */
  @Transactional(readOnly = true)
  public List<SystemConfigRevisionResponse> getVersionHistory(UUID configId) {
    findRootOrThrow(configId);

    return revisionRepository.findHistoryByRootId(configId).stream()
        .map(SystemConfigRevisionResponse::from)
        .toList();
  }

  /**
   * 특정 버전 상세 조회.
   */
  @Transactional(readOnly = true)
  public SystemConfigRevisionResponse getVersion(UUID configId, Integer versionNumber) {
    SystemConfigRevision revision = revisionRepository
        .findByRootIdAndVersion(configId, versionNumber)
        .orElseThrow(() -> new SystemConfigNotFoundException("버전을 찾을 수 없습니다: " + versionNumber));

    return SystemConfigRevisionResponse.from(revision);
  }

  /**
   * 특정 시점의 버전 조회 (Point-in-Time Query).
   */
  @Transactional(readOnly = true)
  public SystemConfigRevisionResponse getVersionAsOf(UUID configId, OffsetDateTime asOf) {
    SystemConfigRevision revision = revisionRepository
        .findByRootIdAsOf(configId, asOf)
        .orElseThrow(() -> new SystemConfigNotFoundException("해당 시점에 유효한 버전이 없습니다: " + asOf));

    return SystemConfigRevisionResponse.from(revision);
  }

  /**
   * 두 버전 비교.
   */
  @Transactional(readOnly = true)
  public SystemConfigCompareResponse compareVersions(UUID configId, Integer version1, Integer version2) {
    List<SystemConfigRevision> revisions = revisionRepository.findVersionsForComparison(
        configId, version1, version2);

    if (revisions.size() != 2) {
      throw new SystemConfigNotFoundException("비교할 버전을 찾을 수 없습니다.");
    }

    SystemConfigRevision v1 = revisions.get(0);
    SystemConfigRevision v2 = revisions.get(1);

    return SystemConfigCompareResponse.from(v1, v2);
  }

  // ==========================================================================
  // 버전 롤백
  // ==========================================================================

  /**
   * 특정 버전으로 롤백.
   */
  public SystemConfigRevisionResponse rollbackToVersion(UUID configId,
      Integer targetVersion,
      String changeReason,
      AuthContext context) {
    SystemConfigRoot root = findRootOrThrow(configId);
    OffsetDateTime now = OffsetDateTime.now();

    // 롤백 대상 버전 조회
    SystemConfigRevision targetRevision = revisionRepository
        .findByRootIdAndVersion(configId, targetVersion)
        .orElseThrow(() -> new SystemConfigNotFoundException("롤백할 버전을 찾을 수 없습니다: " + targetVersion));

    // 현재 버전 종료
    SystemConfigRevision currentVersion = root.getCurrentVersion();
    if (currentVersion != null) {
      currentVersion.close(now);
    }

    // 새 버전 생성 (롤백)
    int nextVersionNumber = revisionRepository.findMaxVersionByRootId(configId) + 1;
    SystemConfigRevision rollbackVersion = SystemConfigRevision.createFromRollback(
        root,
        nextVersionNumber,
        targetRevision.getYamlContent(),
        targetRevision.isActive(),
        changeReason,
        context.username(),
        context.username(),
        now,
        targetVersion
    );

    rollbackVersion = revisionRepository.save(rollbackVersion);
    root.activateNewVersion(rollbackVersion, now);

    // 설정 롤백 알림
    notifySettingsChanged(root.getConfigCode(), targetRevision.getYamlContent());

    return SystemConfigRevisionResponse.from(rollbackVersion);
  }

  // ==========================================================================
  // Draft/Published
  // ==========================================================================

  /**
   * 초안 생성 또는 수정.
   */
  public SystemConfigRevisionResponse saveDraft(UUID configId,
      SystemConfigDraftRequest request,
      AuthContext context) {
    SystemConfigRoot root = findRootOrThrow(configId);
    OffsetDateTime now = OffsetDateTime.now();

    // 기존 초안이 있는지 확인
    SystemConfigRevision draft = revisionRepository.findDraftByRootId(configId)
        .orElse(null);

    if (draft != null) {
      // 기존 초안 수정
      draft.updateDraft(
          request.yamlContent(),
          request.active(),
          request.changeReason(),
          now
      );
    } else {
      // 새 초안 생성
      int nextVersionNumber = revisionRepository.findMaxVersionByRootId(configId) + 1;
      draft = SystemConfigRevision.createDraft(
          root,
          nextVersionNumber,
          request.yamlContent(),
          request.active(),
          request.changeReason(),
          context.username(),
          context.username(),
          now
      );

      draft = revisionRepository.save(draft);
      root.setDraftVersion(draft);
    }

    return SystemConfigRevisionResponse.from(draft);
  }

  /**
   * 초안 조회.
   */
  @Transactional(readOnly = true)
  public SystemConfigRevisionResponse getDraft(UUID configId) {
    SystemConfigRevision draft = revisionRepository.findDraftByRootId(configId)
        .orElseThrow(() -> new SystemConfigNotFoundException("초안이 없습니다."));

    return SystemConfigRevisionResponse.from(draft);
  }

  /**
   * 초안이 있는지 확인.
   */
  @Transactional(readOnly = true)
  public boolean hasDraft(UUID configId) {
    return revisionRepository.existsDraftByRootId(configId);
  }

  /**
   * 초안 게시 (적용).
   */
  public SystemConfigRevisionResponse publishDraft(UUID configId, AuthContext context) {
    SystemConfigRoot root = findRootOrThrow(configId);
    OffsetDateTime now = OffsetDateTime.now();

    SystemConfigRevision draft = revisionRepository.findDraftByRootId(configId)
        .orElseThrow(() -> new SystemConfigNotFoundException("게시할 초안이 없습니다."));

    // 현재 버전 종료
    SystemConfigRevision currentVersion = root.getCurrentVersion();
    if (currentVersion != null) {
      currentVersion.close(now);
    }

    // 초안 게시
    draft.publish(now);
    root.activateNewVersion(draft, now);

    // 설정 게시 알림
    notifySettingsChanged(root.getConfigCode(), draft.getYamlContent());

    return SystemConfigRevisionResponse.from(draft);
  }

  /**
   * 초안 삭제 (취소).
   */
  public void discardDraft(UUID configId) {
    SystemConfigRoot root = findRootOrThrow(configId);

    SystemConfigRevision draft = revisionRepository.findDraftByRootId(configId)
        .orElseThrow(() -> new SystemConfigNotFoundException("삭제할 초안이 없습니다."));

    root.discardDraft();
    revisionRepository.delete(draft);
  }


  /**
   * ID로 설정 조회.
   */
  @Transactional(readOnly = true)
  public SystemConfigRootResponse getConfig(UUID configId) {
    SystemConfigRoot root = findRootOrThrow(configId);
    return SystemConfigRootResponse.from(root);
  }

  /**
   * 설정 메타정보(이름, 설명)만 수정.
   */
  @Transactional
  public SystemConfigRootResponse updateConfigInfo(UUID configId, String name, String description) {
    SystemConfigRoot root = findRootOrThrow(configId);
    root.updateInfo(name, description, OffsetDateTime.now());
    return SystemConfigRootResponse.from(root);
  }

  /**
   * 초안과 현재 버전 비교.
   */
  @Transactional(readOnly = true)
  public SystemConfigCompareResponse compareDraftWithCurrent(UUID configId) {
    SystemConfigRoot root = findRootOrThrow(configId);

    SystemConfigRevision current = root.getCurrentVersion();
    if (current == null) {
      throw new SystemConfigNotFoundException("현재 버전이 없습니다.");
    }

    SystemConfigRevision draft = revisionRepository.findDraftByRootId(configId)
        .orElseThrow(() -> new SystemConfigNotFoundException("비교할 초안이 없습니다."));

    return SystemConfigCompareResponse.from(current, draft);
  }

  // ==========================================================================
  // 헬퍼 메서드
  // ==========================================================================

  private SystemConfigRoot findRootOrThrow(UUID id) {
    return rootRepository.findById(id)
        .orElseThrow(() -> new SystemConfigNotFoundException("시스템 설정을 찾을 수 없습니다."));
  }

  /**
   * 설정 코드로 현재 활성 YAML 내용 조회 (외부 모듈용).
   */
  @Transactional(readOnly = true)
  public String getActiveYamlContent(String configCode) {
    SystemConfigRoot root = rootRepository.findByConfigCode(configCode)
        .orElseThrow(() -> new SystemConfigNotFoundException("설정을 찾을 수 없습니다: " + configCode));

    SystemConfigRevision currentVersion = root.getCurrentVersion();
    if (currentVersion == null || !currentVersion.isActive()) {
      throw new SystemConfigNotFoundException("활성화된 설정이 없습니다: " + configCode);
    }

    return currentVersion.getYamlContent();
  }
}
