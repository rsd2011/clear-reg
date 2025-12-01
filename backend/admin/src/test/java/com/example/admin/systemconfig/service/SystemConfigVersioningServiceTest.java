package com.example.admin.systemconfig.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import com.example.admin.permission.context.AuthContext;
import com.example.admin.systemconfig.domain.SystemConfigRevision;
import com.example.admin.systemconfig.domain.SystemConfigRoot;
import com.example.admin.systemconfig.dto.SystemConfigDraftRequest;
import com.example.admin.systemconfig.dto.SystemConfigRevisionResponse;
import com.example.admin.systemconfig.dto.SystemConfigRootRequest;
import com.example.admin.systemconfig.dto.SystemConfigRootResponse;
import com.example.admin.systemconfig.exception.SystemConfigNotFoundException;
import com.example.admin.systemconfig.repository.SystemConfigRevisionRepository;
import com.example.admin.systemconfig.repository.SystemConfigRootRepository;
import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;

@ExtendWith(MockitoExtension.class)
class SystemConfigVersioningServiceTest {

  @Mock
  private SystemConfigRootRepository rootRepository;

  @Mock
  private SystemConfigRevisionRepository revisionRepository;

  @Mock
  private ObjectProvider<SystemConfigPolicySettingsProvider> settingsProviderProvider;

  private SystemConfigVersioningService service;

  private AuthContext authContext;

  @BeforeEach
  void setUp() {
    service = new SystemConfigVersioningService(rootRepository, revisionRepository, settingsProviderProvider);
    authContext = AuthContext.of("admin", "ORG001", "ADMIN_GROUP",
        FeatureCode.POLICY, ActionCode.UPDATE);
  }

  @Nested
  @DisplayName("getAllConfigs 테스트")
  class GetAllConfigsTest {

    @Test
    @DisplayName("Given: 저장된 설정이 있을 때, When: getAllConfigs 호출, Then: 모든 설정 반환")
    void shouldReturnAllConfigs() {
      // Given
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root1 = SystemConfigRoot.create("auth.settings", "인증 설정", null, now);
      SystemConfigRoot root2 = SystemConfigRoot.create("file.settings", "파일 설정", null, now);

      when(rootRepository.findAll()).thenReturn(List.of(root1, root2));

      // When
      List<SystemConfigRootResponse> result = service.getAllConfigs();

      // Then
      assertThat(result).hasSize(2);
      assertThat(result.get(0).configCode()).isEqualTo("auth.settings");
      assertThat(result.get(1).configCode()).isEqualTo("file.settings");
    }
  }

  @Nested
  @DisplayName("getConfigByCode 테스트")
  class GetConfigByCodeTest {

    @Test
    @DisplayName("Given: 존재하는 설정 코드, When: getConfigByCode 호출, Then: 설정 반환")
    void shouldReturnConfigByCode() {
      // Given
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("auth.settings", "인증 설정", "인증 관련 설정", now);

      when(rootRepository.findByConfigCode("auth.settings")).thenReturn(Optional.of(root));

      // When
      SystemConfigRootResponse result = service.getConfigByCode("auth.settings");

      // Then
      assertThat(result.configCode()).isEqualTo("auth.settings");
      assertThat(result.name()).isEqualTo("인증 설정");
      assertThat(result.description()).isEqualTo("인증 관련 설정");
    }

    @Test
    @DisplayName("Given: 존재하지 않는 설정 코드, When: getConfigByCode 호출, Then: 예외 발생")
    void shouldThrowExceptionWhenConfigNotFound() {
      // Given
      when(rootRepository.findByConfigCode("unknown")).thenReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> service.getConfigByCode("unknown"))
          .isInstanceOf(SystemConfigNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("createConfig 테스트")
  class CreateConfigTest {

    @Test
    @DisplayName("Given: 유효한 요청, When: createConfig 호출, Then: 설정과 첫 번째 버전 생성")
    void shouldCreateConfigWithInitialVersion() {
      // Given
      SystemConfigRootRequest request = new SystemConfigRootRequest(
          "auth.settings", "인증 설정", "인증 관련 설정", "yaml: content", true);

      when(rootRepository.save(any(SystemConfigRoot.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(revisionRepository.save(any(SystemConfigRevision.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // When
      SystemConfigRootResponse result = service.createConfig(request, authContext);

      // Then
      assertThat(result.configCode()).isEqualTo("auth.settings");
      assertThat(result.name()).isEqualTo("인증 설정");

      ArgumentCaptor<SystemConfigRoot> rootCaptor = ArgumentCaptor.forClass(SystemConfigRoot.class);
      verify(rootRepository).save(rootCaptor.capture());
      assertThat(rootCaptor.getValue().getConfigCode()).isEqualTo("auth.settings");

      ArgumentCaptor<SystemConfigRevision> revisionCaptor = ArgumentCaptor.forClass(SystemConfigRevision.class);
      verify(revisionRepository).save(revisionCaptor.capture());
      assertThat(revisionCaptor.getValue().getVersion()).isEqualTo(1);
      assertThat(revisionCaptor.getValue().getChangeAction()).isEqualTo(ChangeAction.CREATE);
    }
  }

  @Nested
  @DisplayName("updateConfig 테스트")
  class UpdateConfigTest {

    @Test
    @DisplayName("Given: 존재하는 설정, When: updateConfig 호출, Then: 새 버전 생성")
    void shouldCreateNewVersionOnUpdate() {
      // Given
      UUID configId = UUID.randomUUID();
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("auth.settings", "인증 설정", null, now);

      SystemConfigRevision currentVersion = SystemConfigRevision.create(
          root, 1, "yaml: v1", true, ChangeAction.CREATE, null, "admin", "관리자", now);
      root.activateNewVersion(currentVersion, now);

      SystemConfigDraftRequest request = new SystemConfigDraftRequest(
          "yaml: v2", true, "설정 업데이트");

      when(rootRepository.findById(configId)).thenReturn(Optional.of(root));
      when(revisionRepository.findMaxVersionByRootId(configId)).thenReturn(1);
      when(revisionRepository.save(any(SystemConfigRevision.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // When
      SystemConfigRevisionResponse result = service.updateConfig(configId, request, authContext);

      // Then
      assertThat(result.version()).isEqualTo(2);
      assertThat(result.yamlContent()).isEqualTo("yaml: v2");
      assertThat(result.changeAction()).isEqualTo(ChangeAction.UPDATE);
    }
  }

  @Nested
  @DisplayName("getVersionHistory 테스트")
  class GetVersionHistoryTest {

    @Test
    @DisplayName("Given: 버전 이력이 있는 설정, When: getVersionHistory 호출, Then: 이력 목록 반환")
    void shouldReturnVersionHistory() {
      // Given
      UUID configId = UUID.randomUUID();
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("auth.settings", "인증 설정", null, now);

      SystemConfigRevision v1 = SystemConfigRevision.create(
          root, 1, "yaml: v1", true, ChangeAction.CREATE, null, "admin", "관리자", now);
      SystemConfigRevision v2 = SystemConfigRevision.create(
          root, 2, "yaml: v2", true, ChangeAction.UPDATE, null, "admin", "관리자", now.plusMinutes(10));

      when(rootRepository.findById(configId)).thenReturn(Optional.of(root));
      when(revisionRepository.findHistoryByRootId(configId)).thenReturn(List.of(v2, v1));

      // When
      List<SystemConfigRevisionResponse> result = service.getVersionHistory(configId);

      // Then
      assertThat(result).hasSize(2);
      assertThat(result.get(0).version()).isEqualTo(2);
      assertThat(result.get(1).version()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("getVersionAsOf 테스트")
  class GetVersionAsOfTest {

    @Test
    @DisplayName("Given: 특정 시점에 유효한 버전이 있을 때, When: getVersionAsOf 호출, Then: 해당 버전 반환")
    void shouldReturnVersionAsOf() {
      // Given
      UUID configId = UUID.randomUUID();
      OffsetDateTime now = OffsetDateTime.now();
      OffsetDateTime asOf = now.plusMinutes(5);
      SystemConfigRoot root = SystemConfigRoot.create("auth.settings", "인증 설정", null, now);

      SystemConfigRevision version = SystemConfigRevision.create(
          root, 1, "yaml: v1", true, ChangeAction.CREATE, null, "admin", "관리자", now);

      when(revisionRepository.findByRootIdAsOf(configId, asOf)).thenReturn(Optional.of(version));

      // When
      SystemConfigRevisionResponse result = service.getVersionAsOf(configId, asOf);

      // Then
      assertThat(result.version()).isEqualTo(1);
    }

    @Test
    @DisplayName("Given: 특정 시점에 유효한 버전이 없을 때, When: getVersionAsOf 호출, Then: 예외 발생")
    void shouldThrowExceptionWhenNoVersionAsOf() {
      // Given
      UUID configId = UUID.randomUUID();
      OffsetDateTime now = OffsetDateTime.now();
      OffsetDateTime asOf = now.minusDays(1);

      when(revisionRepository.findByRootIdAsOf(configId, asOf)).thenReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> service.getVersionAsOf(configId, asOf))
          .isInstanceOf(SystemConfigNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("rollbackToVersion 테스트")
  class RollbackToVersionTest {

    @Test
    @DisplayName("Given: 롤백 대상 버전이 존재할 때, When: rollbackToVersion 호출, Then: 롤백 버전 생성")
    void shouldRollbackToVersion() {
      // Given
      UUID configId = UUID.randomUUID();
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("auth.settings", "인증 설정", null, now);

      SystemConfigRevision v1 = SystemConfigRevision.create(
          root, 1, "yaml: v1", true, ChangeAction.CREATE, null, "admin", "관리자", now);
      SystemConfigRevision v2 = SystemConfigRevision.create(
          root, 2, "yaml: v2", true, ChangeAction.UPDATE, null, "admin", "관리자", now.plusMinutes(10));
      root.activateNewVersion(v1, now);
      root.activateNewVersion(v2, now.plusMinutes(10));

      when(rootRepository.findById(configId)).thenReturn(Optional.of(root));
      when(revisionRepository.findByRootIdAndVersion(configId, 1)).thenReturn(Optional.of(v1));
      when(revisionRepository.findMaxVersionByRootId(configId)).thenReturn(2);
      when(revisionRepository.save(any(SystemConfigRevision.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // When
      SystemConfigRevisionResponse result = service.rollbackToVersion(configId, 1, "버전 1로 롤백", authContext);

      // Then
      assertThat(result.version()).isEqualTo(3);
      assertThat(result.yamlContent()).isEqualTo("yaml: v1");
      assertThat(result.changeAction()).isEqualTo(ChangeAction.ROLLBACK);
    }
  }

  @Nested
  @DisplayName("Draft 워크플로우 테스트")
  class DraftWorkflowTest {

    @Test
    @DisplayName("Given: 존재하는 설정, When: saveDraft 호출, Then: 초안 생성")
    void shouldSaveDraft() {
      // Given
      UUID configId = UUID.randomUUID();
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("auth.settings", "인증 설정", null, now);

      SystemConfigRevision currentVersion = SystemConfigRevision.create(
          root, 1, "yaml: v1", true, ChangeAction.CREATE, null, "admin", "관리자", now);
      root.activateNewVersion(currentVersion, now);

      SystemConfigDraftRequest request = new SystemConfigDraftRequest(
          "yaml: draft", true, "초안 작성");

      when(rootRepository.findById(configId)).thenReturn(Optional.of(root));
      when(revisionRepository.findDraftByRootId(configId)).thenReturn(Optional.empty());
      when(revisionRepository.findMaxVersionByRootId(configId)).thenReturn(1);
      when(revisionRepository.save(any(SystemConfigRevision.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // When
      SystemConfigRevisionResponse result = service.saveDraft(configId, request, authContext);

      // Then
      assertThat(result.version()).isEqualTo(2);
      assertThat(result.status()).isEqualTo(VersionStatus.DRAFT);
      assertThat(result.yamlContent()).isEqualTo("yaml: draft");
    }

    @Test
    @DisplayName("Given: 기존 초안이 있을 때, When: saveDraft 호출, Then: 초안 업데이트")
    void shouldUpdateExistingDraft() {
      // Given
      UUID configId = UUID.randomUUID();
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("auth.settings", "인증 설정", null, now);

      SystemConfigRevision existingDraft = SystemConfigRevision.createDraft(
          root, 2, "yaml: old draft", true, "초기 초안", "admin", "관리자", now);

      SystemConfigDraftRequest request = new SystemConfigDraftRequest(
          "yaml: updated draft", true, "초안 수정");

      when(rootRepository.findById(configId)).thenReturn(Optional.of(root));
      when(revisionRepository.findDraftByRootId(configId)).thenReturn(Optional.of(existingDraft));

      // When
      SystemConfigRevisionResponse result = service.saveDraft(configId, request, authContext);

      // Then
      assertThat(result.yamlContent()).isEqualTo("yaml: updated draft");
      assertThat(result.changeReason()).isEqualTo("초안 수정");
    }

    @Test
    @DisplayName("Given: 초안이 있을 때, When: publishDraft 호출, Then: 초안이 게시됨")
    void shouldPublishDraft() {
      // Given
      UUID configId = UUID.randomUUID();
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("auth.settings", "인증 설정", null, now);

      SystemConfigRevision currentVersion = SystemConfigRevision.create(
          root, 1, "yaml: v1", true, ChangeAction.CREATE, null, "admin", "관리자", now);
      root.activateNewVersion(currentVersion, now);

      SystemConfigRevision draft = SystemConfigRevision.createDraft(
          root, 2, "yaml: draft", true, "초안", "admin", "관리자", now);
      root.setDraftVersion(draft);

      when(rootRepository.findById(configId)).thenReturn(Optional.of(root));
      when(revisionRepository.findDraftByRootId(configId)).thenReturn(Optional.of(draft));

      // When
      SystemConfigRevisionResponse result = service.publishDraft(configId, authContext);

      // Then
      assertThat(result.status()).isEqualTo(VersionStatus.PUBLISHED);
      assertThat(result.changeAction()).isEqualTo(ChangeAction.PUBLISH);
    }

    @Test
    @DisplayName("Given: 초안이 있을 때, When: discardDraft 호출, Then: 초안 삭제")
    void shouldDiscardDraft() {
      // Given
      UUID configId = UUID.randomUUID();
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("auth.settings", "인증 설정", null, now);

      SystemConfigRevision draft = SystemConfigRevision.createDraft(
          root, 2, "yaml: draft", true, "초안", "admin", "관리자", now);
      root.setDraftVersion(draft);

      when(rootRepository.findById(configId)).thenReturn(Optional.of(root));
      when(revisionRepository.findDraftByRootId(configId)).thenReturn(Optional.of(draft));

      // When
      service.discardDraft(configId);

      // Then
      verify(revisionRepository).delete(draft);
      assertThat(root.hasDraft()).isFalse();
    }

    @Test
    @DisplayName("Given: 초안이 없을 때, When: discardDraft 호출, Then: 예외 발생")
    void shouldThrowExceptionWhenNoDraftToDiscard() {
      // Given
      UUID configId = UUID.randomUUID();
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("auth.settings", "인증 설정", null, now);

      when(rootRepository.findById(configId)).thenReturn(Optional.of(root));
      when(revisionRepository.findDraftByRootId(configId)).thenReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> service.discardDraft(configId))
          .isInstanceOf(SystemConfigNotFoundException.class);
    }

    @Test
    @DisplayName("Given: 초안이 있을 때, When: hasDraft 호출, Then: true 반환")
    void shouldReturnTrueWhenDraftExists() {
      // Given
      UUID configId = UUID.randomUUID();

      when(revisionRepository.existsDraftByRootId(configId)).thenReturn(true);

      // When
      boolean result = service.hasDraft(configId);

      // Then
      assertThat(result).isTrue();
    }
  }

  @Nested
  @DisplayName("deleteConfig 테스트")
  class DeleteConfigTest {

    @Test
    @DisplayName("Given: 존재하는 설정, When: deleteConfig 호출, Then: 비활성화 버전 생성")
    void shouldCreateDeleteVersion() {
      // Given
      UUID configId = UUID.randomUUID();
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("auth.settings", "인증 설정", null, now);

      SystemConfigRevision currentVersion = SystemConfigRevision.create(
          root, 1, "yaml: v1", true, ChangeAction.CREATE, null, "admin", "관리자", now);
      root.activateNewVersion(currentVersion, now);

      when(rootRepository.findById(configId)).thenReturn(Optional.of(root));
      when(revisionRepository.findMaxVersionByRootId(configId)).thenReturn(1);
      when(revisionRepository.save(any(SystemConfigRevision.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // When
      SystemConfigRevisionResponse result = service.deleteConfig(configId, authContext);

      // Then
      assertThat(result.version()).isEqualTo(2);
      assertThat(result.active()).isFalse();
      assertThat(result.changeAction()).isEqualTo(ChangeAction.DELETE);
    }
  }
}
