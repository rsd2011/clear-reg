package com.example.admin.systemconfig.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;

class SystemConfigRootTest {

  @Nested
  @DisplayName("create 메서드 테스트")
  class CreateTest {

    @Test
    @DisplayName("Given: 유효한 설정 정보, When: create 호출, Then: 루트 엔티티 생성")
    void shouldCreateRootWithValidData() {
      // Given
      String configCode = "auth.settings";
      String name = "인증 설정";
      String description = "인증 관련 시스템 설정";
      OffsetDateTime now = OffsetDateTime.now();

      // When
      SystemConfigRoot root = SystemConfigRoot.create(configCode, name, description, now);

      // Then
      assertThat(root.getConfigCode()).isEqualTo(configCode);
      assertThat(root.getName()).isEqualTo(name);
      assertThat(root.getDescription()).isEqualTo(description);
      assertThat(root.getCreatedAt()).isEqualTo(now);
      assertThat(root.getUpdatedAt()).isEqualTo(now);
      assertThat(root.getCurrentVersion()).isNull();
      assertThat(root.getPreviousVersion()).isNull();
      assertThat(root.getNextVersion()).isNull();
    }
  }

  @Nested
  @DisplayName("버전 관리 테스트")
  class VersionManagementTest {

    @Test
    @DisplayName("Given: 루트와 버전, When: activateNewVersion 호출, Then: 현재 버전이 업데이트됨")
    void shouldActivateNewVersion() {
      // Given
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("test.settings", "테스트", null, now);

      SystemConfigRevision version1 = SystemConfigRevision.create(
          root, 1, "yaml: content", true, ChangeAction.CREATE, null, "admin", "관리자", now);

      // When
      root.activateNewVersion(version1, now);

      // Then
      assertThat(root.getCurrentVersion()).isEqualTo(version1);
      assertThat(root.getPreviousVersion()).isNull();
      assertThat(root.getNextVersion()).isNull();
      assertThat(root.getCurrentVersionNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("Given: 현재 버전이 있는 상태, When: activateNewVersion 호출, Then: 이전 버전으로 이동")
    void shouldMovePreviousVersionWhenActivatingNew() {
      // Given
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("test.settings", "테스트", null, now);

      SystemConfigRevision version1 = SystemConfigRevision.create(
          root, 1, "yaml: v1", true, ChangeAction.CREATE, null, "admin", "관리자", now);
      root.activateNewVersion(version1, now);

      SystemConfigRevision version2 = SystemConfigRevision.create(
          root, 2, "yaml: v2", true, ChangeAction.UPDATE, null, "admin", "관리자", now);

      // When
      root.activateNewVersion(version2, now.plusMinutes(1));

      // Then
      assertThat(root.getCurrentVersion()).isEqualTo(version2);
      assertThat(root.getPreviousVersion()).isEqualTo(version1);
      assertThat(root.getCurrentVersionNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("Given: 초안이 없는 상태, When: hasDraft 호출, Then: false 반환")
    void shouldReturnFalseWhenNoDraft() {
      // Given
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("test.settings", "테스트", null, now);

      // When & Then
      assertThat(root.hasDraft()).isFalse();
    }

    @Test
    @DisplayName("Given: 초안이 있는 상태, When: hasDraft 호출, Then: true 반환")
    void shouldReturnTrueWhenHasDraft() {
      // Given
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("test.settings", "테스트", null, now);

      SystemConfigRevision draft = SystemConfigRevision.createDraft(
          root, 1, "yaml: draft", true, "초안 생성", "admin", "관리자", now);
      root.setDraftVersion(draft);

      // When & Then
      assertThat(root.hasDraft()).isTrue();
    }

    @Test
    @DisplayName("Given: 초안이 없는 상태, When: publishDraft 호출, Then: 예외 발생")
    void shouldThrowExceptionWhenPublishingWithoutDraft() {
      // Given
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("test.settings", "테스트", null, now);

      // When & Then
      assertThatThrownBy(() -> root.publishDraft(now))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("게시할 초안 버전이 없습니다");
    }

    @Test
    @DisplayName("Given: 초안이 있는 상태, When: discardDraft 호출, Then: 초안이 제거됨")
    void shouldDiscardDraft() {
      // Given
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("test.settings", "테스트", null, now);

      SystemConfigRevision draft = SystemConfigRevision.createDraft(
          root, 1, "yaml: draft", true, "초안 생성", "admin", "관리자", now);
      root.setDraftVersion(draft);

      // When
      root.discardDraft();

      // Then
      assertThat(root.hasDraft()).isFalse();
      assertThat(root.getNextVersion()).isNull();
    }
  }

  @Nested
  @DisplayName("편의 메서드 테스트")
  class ConvenienceMethodsTest {

    @Test
    @DisplayName("Given: 현재 버전이 없는 상태, When: getYamlContent 호출, Then: null 반환")
    void shouldReturnNullWhenNoCurrentVersion() {
      // Given
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("test.settings", "테스트", null, now);

      // When & Then
      assertThat(root.getYamlContent()).isNull();
      assertThat(root.isActive()).isFalse();
    }

    @Test
    @DisplayName("Given: 현재 버전이 있는 상태, When: getYamlContent 호출, Then: YAML 내용 반환")
    void shouldReturnYamlContentFromCurrentVersion() {
      // Given
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("test.settings", "테스트", null, now);

      String yamlContent = "authentication:\n  enabled: true";
      SystemConfigRevision version = SystemConfigRevision.create(
          root, 1, yamlContent, true, ChangeAction.CREATE, null, "admin", "관리자", now);
      root.activateNewVersion(version, now);

      // When & Then
      assertThat(root.getYamlContent()).isEqualTo(yamlContent);
      assertThat(root.isActive()).isTrue();
    }
  }
}
