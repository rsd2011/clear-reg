package com.example.admin.systemconfig.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;

class SystemConfigRevisionTest {

  @Nested
  @DisplayName("create 메서드 테스트")
  class CreateTest {

    @Test
    @DisplayName("Given: 유효한 설정 정보, When: create 호출, Then: PUBLISHED 상태로 리비전 생성")
    void shouldCreatePublishedRevision() {
      // Given
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("auth.settings", "인증 설정", null, now);
      String yamlContent = "authentication:\n  enabled: true";

      // When
      SystemConfigRevision revision = SystemConfigRevision.create(
          root, 1, yamlContent, true, ChangeAction.CREATE, "초기 생성", "admin", "관리자", now);

      // Then
      assertThat(revision.getRoot()).isEqualTo(root);
      assertThat(revision.getVersion()).isEqualTo(1);
      assertThat(revision.getYamlContent()).isEqualTo(yamlContent);
      assertThat(revision.isActive()).isTrue();
      assertThat(revision.getStatus()).isEqualTo(VersionStatus.PUBLISHED);
      assertThat(revision.getChangeAction()).isEqualTo(ChangeAction.CREATE);
      assertThat(revision.getChangeReason()).isEqualTo("초기 생성");
      assertThat(revision.getChangedBy()).isEqualTo("admin");
      assertThat(revision.getChangedByName()).isEqualTo("관리자");
      assertThat(revision.getValidFrom()).isEqualTo(now);
      assertThat(revision.getValidTo()).isNull();
      assertThat(revision.isCurrent()).isTrue();
      assertThat(revision.isDraft()).isFalse();
    }
  }

  @Nested
  @DisplayName("createDraft 메서드 테스트")
  class CreateDraftTest {

    @Test
    @DisplayName("Given: 유효한 정보, When: createDraft 호출, Then: DRAFT 상태로 리비전 생성")
    void shouldCreateDraftRevision() {
      // Given
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("auth.settings", "인증 설정", null, now);
      String yamlContent = "authentication:\n  enabled: false";

      // When
      SystemConfigRevision draft = SystemConfigRevision.createDraft(
          root, 2, yamlContent, true, "설정 변경 초안", "admin", "관리자", now);

      // Then
      assertThat(draft.getVersion()).isEqualTo(2);
      assertThat(draft.getStatus()).isEqualTo(VersionStatus.DRAFT);
      assertThat(draft.getChangeAction()).isEqualTo(ChangeAction.DRAFT);
      assertThat(draft.isDraft()).isTrue();
      assertThat(draft.isCurrent()).isFalse();
    }
  }

  @Nested
  @DisplayName("createFromRollback 메서드 테스트")
  class CreateFromRollbackTest {

    @Test
    @DisplayName("Given: 롤백 정보, When: createFromRollback 호출, Then: ROLLBACK 액션으로 리비전 생성")
    void shouldCreateRollbackRevision() {
      // Given
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("auth.settings", "인증 설정", null, now);
      String yamlContent = "authentication:\n  enabled: true";

      // When
      SystemConfigRevision rollback = SystemConfigRevision.createFromRollback(
          root, 3, yamlContent, true, "버전 1로 롤백", "admin", "관리자", now, 1);

      // Then
      assertThat(rollback.getVersion()).isEqualTo(3);
      assertThat(rollback.getStatus()).isEqualTo(VersionStatus.PUBLISHED);
      assertThat(rollback.getChangeAction()).isEqualTo(ChangeAction.ROLLBACK);
      assertThat(rollback.getRollbackFromVersion()).isEqualTo(1);
      assertThat(rollback.isCurrent()).isTrue();
    }
  }

  @Nested
  @DisplayName("close 메서드 테스트")
  class CloseTest {

    @Test
    @DisplayName("Given: PUBLISHED 상태 리비전, When: close 호출, Then: HISTORICAL로 변경")
    void shouldClosePublishedRevision() {
      // Given
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("auth.settings", "인증 설정", null, now);
      SystemConfigRevision revision = SystemConfigRevision.create(
          root, 1, "yaml: v1", true, ChangeAction.CREATE, null, "admin", "관리자", now);

      OffsetDateTime closedAt = now.plusMinutes(10);

      // When
      revision.close(closedAt);

      // Then
      assertThat(revision.getValidTo()).isEqualTo(closedAt);
      assertThat(revision.getStatus()).isEqualTo(VersionStatus.HISTORICAL);
      assertThat(revision.isCurrent()).isFalse();
    }

    @Test
    @DisplayName("Given: DRAFT 상태 리비전, When: close 호출, Then: validTo만 설정되고 상태는 유지")
    void shouldNotChangeStatusWhenClosingDraft() {
      // Given
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("auth.settings", "인증 설정", null, now);
      SystemConfigRevision draft = SystemConfigRevision.createDraft(
          root, 2, "yaml: draft", true, "초안", "admin", "관리자", now);

      OffsetDateTime closedAt = now.plusMinutes(10);

      // When
      draft.close(closedAt);

      // Then
      assertThat(draft.getValidTo()).isEqualTo(closedAt);
      assertThat(draft.getStatus()).isEqualTo(VersionStatus.DRAFT);
    }
  }

  @Nested
  @DisplayName("publish 메서드 테스트")
  class PublishTest {

    @Test
    @DisplayName("Given: DRAFT 상태 리비전, When: publish 호출, Then: PUBLISHED로 변경")
    void shouldPublishDraft() {
      // Given
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("auth.settings", "인증 설정", null, now);
      SystemConfigRevision draft = SystemConfigRevision.createDraft(
          root, 2, "yaml: draft", true, "초안", "admin", "관리자", now);

      OffsetDateTime publishedAt = now.plusMinutes(30);

      // When
      draft.publish(publishedAt);

      // Then
      assertThat(draft.getStatus()).isEqualTo(VersionStatus.PUBLISHED);
      assertThat(draft.getChangeAction()).isEqualTo(ChangeAction.PUBLISH);
      assertThat(draft.getValidFrom()).isEqualTo(publishedAt);
      assertThat(draft.getChangedAt()).isEqualTo(publishedAt);
      assertThat(draft.isCurrent()).isTrue();
    }

    @Test
    @DisplayName("Given: PUBLISHED 상태 리비전, When: publish 호출, Then: 예외 발생")
    void shouldThrowExceptionWhenPublishingNonDraft() {
      // Given
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("auth.settings", "인증 설정", null, now);
      SystemConfigRevision revision = SystemConfigRevision.create(
          root, 1, "yaml: v1", true, ChangeAction.CREATE, null, "admin", "관리자", now);

      // When & Then
      assertThatThrownBy(() -> revision.publish(now.plusMinutes(1)))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("초안 상태에서만 게시할 수 있습니다");
    }
  }

  @Nested
  @DisplayName("updateDraft 메서드 테스트")
  class UpdateDraftTest {

    @Test
    @DisplayName("Given: DRAFT 상태 리비전, When: updateDraft 호출, Then: 내용 업데이트됨")
    void shouldUpdateDraftContent() {
      // Given
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("auth.settings", "인증 설정", null, now);
      SystemConfigRevision draft = SystemConfigRevision.createDraft(
          root, 2, "yaml: original", true, "초안", "admin", "관리자", now);

      String newContent = "yaml: updated";
      OffsetDateTime updateTime = now.plusMinutes(5);

      // When
      draft.updateDraft(newContent, false, "수정된 내용", updateTime);

      // Then
      assertThat(draft.getYamlContent()).isEqualTo(newContent);
      assertThat(draft.isActive()).isFalse();
      assertThat(draft.getChangeReason()).isEqualTo("수정된 내용");
      assertThat(draft.getChangedAt()).isEqualTo(updateTime);
    }

    @Test
    @DisplayName("Given: PUBLISHED 상태 리비전, When: updateDraft 호출, Then: 예외 발생")
    void shouldThrowExceptionWhenUpdatingNonDraft() {
      // Given
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("auth.settings", "인증 설정", null, now);
      SystemConfigRevision revision = SystemConfigRevision.create(
          root, 1, "yaml: v1", true, ChangeAction.CREATE, null, "admin", "관리자", now);

      // When & Then
      assertThatThrownBy(() -> revision.updateDraft("new content", true, "reason", now))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("초안 상태에서만 수정할 수 있습니다");
    }
  }

  @Nested
  @DisplayName("편의 메서드 테스트")
  class ConvenienceMethodsTest {

    @Test
    @DisplayName("Given: 리비전, When: getConfigCode 호출, Then: root의 configCode 반환")
    void shouldReturnConfigCodeFromRoot() {
      // Given
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("auth.settings", "인증 설정", null, now);
      SystemConfigRevision revision = SystemConfigRevision.create(
          root, 1, "yaml: v1", true, ChangeAction.CREATE, null, "admin", "관리자", now);

      // When & Then
      assertThat(revision.getConfigCode()).isEqualTo("auth.settings");
    }

    @Test
    @DisplayName("Given: 버전 태그 설정, When: setVersionTag 호출, Then: 태그 저장됨")
    void shouldSetVersionTag() {
      // Given
      OffsetDateTime now = OffsetDateTime.now();
      SystemConfigRoot root = SystemConfigRoot.create("auth.settings", "인증 설정", null, now);
      SystemConfigRevision revision = SystemConfigRevision.create(
          root, 1, "yaml: v1", true, ChangeAction.CREATE, null, "admin", "관리자", now);

      // When
      revision.setVersionTag("v1.0-release");

      // Then
      assertThat(revision.getVersionTag()).isEqualTo("v1.0-release");
    }
  }
}
