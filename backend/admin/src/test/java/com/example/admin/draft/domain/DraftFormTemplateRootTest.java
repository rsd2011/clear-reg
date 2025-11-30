package com.example.admin.draft.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.orggroup.WorkType;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;

class DraftFormTemplateRootTest {

    @Test
    @DisplayName("DraftFormTemplateRoot.create는 새 루트를 생성한다")
    void createReturnsNewRoot() {
        OffsetDateTime now = OffsetDateTime.now();

        DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

        assertThat(root.getId()).isNotNull();
        assertThat(root.getCreatedAt()).isEqualTo(now);
        assertThat(root.getUpdatedAt()).isEqualTo(now);
        assertThat(root.getCurrentVersion()).isNull();
    }

    @Test
    @DisplayName("activateNewVersion은 새 버전을 활성화하고 이전 버전을 previousVersion으로 이동한다")
    void activateNewVersionReplacesOldVersion() {
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

        DraftFormTemplate v1 = DraftFormTemplate.create(
                root, 1, "V1", WorkType.GENERAL, "{}", true,
                ChangeAction.CREATE, null, "user", "User", now);
        root.activateNewVersion(v1, now);

        assertThat(root.getCurrentVersion()).isEqualTo(v1);
        assertThat(v1.getStatus()).isEqualTo(VersionStatus.PUBLISHED);

        DraftFormTemplate v2 = DraftFormTemplate.create(
                root, 2, "V2", WorkType.GENERAL, "{}", true,
                ChangeAction.UPDATE, null, "user", "User", now.plusSeconds(1));
        root.activateNewVersion(v2, now.plusSeconds(1));

        assertThat(root.getCurrentVersion()).isEqualTo(v2);
        assertThat(root.getPreviousVersion()).isEqualTo(v1);
        // activateNewVersion은 이전 버전의 상태를 변경하지 않음 (호출자가 필요시 close() 호출)
        assertThat(v2.getStatus()).isEqualTo(VersionStatus.PUBLISHED);
    }

    @Test
    @DisplayName("getTemplateCode는 루트 생성 시 UUID 형식의 코드를 반환한다")
    void getTemplateCodeReturnsUuidCode() {
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

        // 루트 생성 시 templateCode가 UUID 문자열로 설정됨
        assertThat(root.getTemplateCode()).isNotNull();
        assertThat(root.getTemplateCode()).hasSize(36); // UUID 형식 (8-4-4-4-12)

        DraftFormTemplate template = DraftFormTemplate.create(
                root, 1, "Template", WorkType.GENERAL, "{}", true,
                ChangeAction.CREATE, null, "user", "User", now);
        root.activateNewVersion(template, now);

        // 템플릿은 루트의 templateCode를 반환
        assertThat(template.getTemplateCode()).isEqualTo(root.getTemplateCode());
    }

    @Test
    @DisplayName("publishDraft는 초안을 게시하고 현재 버전을 종료한다")
    void publishDraftPublishesAndClosesCurrentVersion() {
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

        // 첫 버전 생성 및 활성화
        DraftFormTemplate v1 = DraftFormTemplate.create(
                root, 1, "V1", WorkType.GENERAL, "{}", true,
                ChangeAction.CREATE, null, "user", "User", now);
        root.activateNewVersion(v1, now);

        // 초안 생성 및 설정
        DraftFormTemplate draft = DraftFormTemplate.createDraft(
                root, 2, "V2", WorkType.HR_UPDATE, "{}", true,
                null, "user", "User", now.plusSeconds(1));
        root.setDraftVersion(draft);

        assertThat(root.hasDraft()).isTrue();
        assertThat(draft.getStatus()).isEqualTo(VersionStatus.DRAFT);

        // 초안 게시
        OffsetDateTime publishTime = now.plusSeconds(2);
        root.publishDraft(publishTime);

        // 검증: 이전 버전은 종료되고, 초안은 게시됨
        assertThat(v1.getStatus()).isEqualTo(VersionStatus.HISTORICAL);
        assertThat(v1.getValidTo()).isEqualTo(publishTime);
        assertThat(draft.getStatus()).isEqualTo(VersionStatus.PUBLISHED);
        assertThat(root.getCurrentVersion()).isEqualTo(draft);
        assertThat(root.getPreviousVersion()).isEqualTo(v1);
        assertThat(root.hasDraft()).isFalse();
    }

    @Test
    @DisplayName("createWithCode는 지정된 코드로 루트를 생성한다")
    void createWithCodeCreatesRootWithSpecifiedCode() {
        OffsetDateTime now = OffsetDateTime.now();
        String customCode = "CUSTOM-001";

        DraftFormTemplateRoot root = DraftFormTemplateRoot.createWithCode(customCode, now);

        assertThat(root.getTemplateCode()).isEqualTo(customCode);
    }

    @Test
    @DisplayName("touch는 updatedAt을 갱신한다")
    void touchUpdatesUpdatedAt() {
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

        OffsetDateTime later = now.plusSeconds(10);
        root.touch(later);

        assertThat(root.getUpdatedAt()).isEqualTo(later);
    }

    @Test
    @DisplayName("discardDraft는 초안을 삭제한다")
    void discardDraftRemovesDraft() {
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

        DraftFormTemplate draft = DraftFormTemplate.createDraft(
                root, 1, "Draft", WorkType.GENERAL, "{}", true,
                null, "user", "User", now);
        root.setDraftVersion(draft);

        assertThat(root.hasDraft()).isTrue();

        root.discardDraft();

        assertThat(root.hasDraft()).isFalse();
        assertThat(root.getNextVersion()).isNull();
    }

    @Test
    @DisplayName("canRollback은 이전 버전이 있을 때만 true를 반환한다")
    void canRollbackReturnsTrueOnlyWhenPreviousVersionExists() {
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

        assertThat(root.canRollback()).isFalse();

        DraftFormTemplate v1 = DraftFormTemplate.create(
                root, 1, "V1", WorkType.GENERAL, "{}", true,
                ChangeAction.CREATE, null, "user", "User", now);
        root.activateNewVersion(v1, now);

        assertThat(root.canRollback()).isFalse();

        DraftFormTemplate v2 = DraftFormTemplate.create(
                root, 2, "V2", WorkType.GENERAL, "{}", true,
                ChangeAction.UPDATE, null, "user", "User", now.plusSeconds(1));
        root.activateNewVersion(v2, now.plusSeconds(1));

        assertThat(root.canRollback()).isTrue();
    }

    @Test
    @DisplayName("getCurrentVersionNumber는 현재 버전 번호를 반환한다")
    void getCurrentVersionNumberReturnsCurrentVersion() {
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

        assertThat(root.getCurrentVersionNumber()).isNull();

        DraftFormTemplate v1 = DraftFormTemplate.create(
                root, 1, "V1", WorkType.GENERAL, "{}", true,
                ChangeAction.CREATE, null, "user", "User", now);
        root.activateNewVersion(v1, now);

        assertThat(root.getCurrentVersionNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("편의 메서드들은 현재 버전의 값을 반환한다")
    void convenienceMethodsReturnCurrentVersionValues() {
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

        // 현재 버전이 없으면 null/false 반환
        assertThat(root.getName()).isNull();
        assertThat(root.getWorkType()).isNull();
        assertThat(root.isActive()).isFalse();

        DraftFormTemplate template = DraftFormTemplate.create(
                root, 1, "Template Name", WorkType.HR_UPDATE, "{}", true,
                ChangeAction.CREATE, null, "user", "User", now);
        root.activateNewVersion(template, now);

        assertThat(root.getName()).isEqualTo("Template Name");
        assertThat(root.getWorkType()).isEqualTo(WorkType.HR_UPDATE);
        assertThat(root.isActive()).isTrue();
    }

    @Test
    @DisplayName("publishDraft는 초안이 없으면 예외를 던진다")
    void publishDraftThrowsWhenNoDraft() {
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> root.publishDraft(now))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("게시할 초안 버전이 없습니다");
    }
}
