package com.example.admin.draft.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.orggroup.WorkType;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;

class DraftFormTemplateTest {

    @Test
    @DisplayName("create는 PUBLISHED 상태의 템플릿을 생성한다")
    void createReturnsPublishedTemplate() {
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

        DraftFormTemplate template = DraftFormTemplate.create(
                root, 1, "Name", WorkType.GENERAL, "{\"fields\":[]}", true,
                ChangeAction.CREATE, "신규 생성", "user", "User", now);

        assertThat(template.getId()).isNotNull();
        assertThat(template.getRoot()).isEqualTo(root);
        assertThat(template.getVersion()).isEqualTo(1);
        assertThat(template.getName()).isEqualTo("Name");
        assertThat(template.getWorkType()).isEqualTo(WorkType.GENERAL);
        assertThat(template.getSchemaJson()).isEqualTo("{\"fields\":[]}");
        assertThat(template.isActive()).isTrue();
        assertThat(template.getChangeAction()).isEqualTo(ChangeAction.CREATE);
        assertThat(template.getChangeReason()).isEqualTo("신규 생성");
        assertThat(template.getChangedBy()).isEqualTo("user");
        assertThat(template.getChangedByName()).isEqualTo("User");
        assertThat(template.getStatus()).isEqualTo(VersionStatus.PUBLISHED);
    }

    @Test
    @DisplayName("createDraft는 DRAFT 상태의 템플릿을 생성한다")
    void createDraftReturnsDraftTemplate() {
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

        DraftFormTemplate template = DraftFormTemplate.createDraft(
                root, 1, "Name", WorkType.HR_UPDATE, "{}", true,
                null, "user", "User", now);

        assertThat(template.getStatus()).isEqualTo(VersionStatus.DRAFT);
        assertThat(template.getWorkType()).isEqualTo(WorkType.HR_UPDATE);
        assertThat(template.isDraft()).isTrue();
    }

    @Test
    @DisplayName("publish는 DRAFT 상태를 PUBLISHED로 변경한다")
    void publishChangesDraftToPublished() {
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

        DraftFormTemplate template = DraftFormTemplate.createDraft(
                root, 1, "Name", WorkType.GENERAL, "{}", true,
                null, "user", "User", now);

        assertThat(template.getStatus()).isEqualTo(VersionStatus.DRAFT);

        template.publish(now.plusSeconds(1));

        assertThat(template.getStatus()).isEqualTo(VersionStatus.PUBLISHED);
        assertThat(template.getChangeAction()).isEqualTo(ChangeAction.PUBLISH);
    }

    @Test
    @DisplayName("publish는 DRAFT가 아닌 상태에서 예외를 던진다")
    void publishThrowsWhenNotDraft() {
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

        DraftFormTemplate template = DraftFormTemplate.create(
                root, 1, "Name", WorkType.GENERAL, "{}", true,
                ChangeAction.CREATE, null, "user", "User", now);

        assertThatThrownBy(() -> template.publish(now))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("초안 상태에서만 게시할 수 있습니다");
    }

    @Test
    @DisplayName("close는 버전을 종료하고 HISTORICAL로 변경한다")
    void closeEndsVersionAndMarksHistorical() {
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

        DraftFormTemplate template = DraftFormTemplate.create(
                root, 1, "Name", WorkType.GENERAL, "{}", true,
                ChangeAction.CREATE, null, "user", "User", now);

        template.close(now.plusSeconds(1));

        assertThat(template.getStatus()).isEqualTo(VersionStatus.HISTORICAL);
        assertThat(template.getValidTo()).isEqualTo(now.plusSeconds(1));
    }

    @Test
    @DisplayName("updateDraft는 초안의 필드를 수정한다")
    void updateDraftModifiesFields() {
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

        DraftFormTemplate template = DraftFormTemplate.createDraft(
                root, 1, "Name", WorkType.GENERAL, "{}", true,
                null, "user", "User", now);

        OffsetDateTime later = now.plusSeconds(10);
        template.updateDraft("New Name", WorkType.HR_UPDATE, "{\"updated\":true}", false, "변경 사유", later);

        assertThat(template.getName()).isEqualTo("New Name");
        assertThat(template.getWorkType()).isEqualTo(WorkType.HR_UPDATE);
        assertThat(template.getSchemaJson()).isEqualTo("{\"updated\":true}");
        assertThat(template.isActive()).isFalse();
        assertThat(template.getChangeReason()).isEqualTo("변경 사유");
        assertThat(template.getUpdatedAt()).isEqualTo(later);
    }

    @Test
    @DisplayName("updateDraft는 DRAFT가 아닌 상태에서 예외를 던진다")
    void updateDraftThrowsWhenNotDraft() {
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

        DraftFormTemplate template = DraftFormTemplate.create(
                root, 1, "Name", WorkType.GENERAL, "{}", true,
                ChangeAction.CREATE, null, "user", "User", now);

        assertThatThrownBy(() -> template.updateDraft("New", WorkType.GENERAL, "{}", true, null, now))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("초안 상태에서만 수정할 수 있습니다");
    }

    @Test
    @DisplayName("getTemplateCode는 루트의 templateCode를 반환한다")
    void getTemplateCodeReturnsRootTemplateCode() {
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

        DraftFormTemplate template = DraftFormTemplate.create(
                root, 1, "Name", WorkType.GENERAL, "{}", true,
                ChangeAction.CREATE, null, "user", "User", now);

        // root.getTemplateCode()는 UUID 문자열을 반환
        assertThat(template.getTemplateCode()).isEqualTo(root.getTemplateCode());
        assertThat(template.getTemplateCode()).isNotNull();
    }

    @Test
    @DisplayName("isCurrent는 validTo가 null이고 PUBLISHED일 때만 true를 반환한다")
    void isCurrentReturnsTrueOnlyWhenValidToIsNullAndPublished() {
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

        DraftFormTemplate published = DraftFormTemplate.create(
                root, 1, "Name", WorkType.GENERAL, "{}", true,
                ChangeAction.CREATE, null, "user", "User", now);

        assertThat(published.isCurrent()).isTrue();

        published.close(now.plusSeconds(1));
        assertThat(published.isCurrent()).isFalse();
    }

    @Test
    @DisplayName("createFromRollback은 롤백 버전을 생성한다")
    void createFromRollbackCreatesRollbackVersion() {
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

        DraftFormTemplate rollback = DraftFormTemplate.createFromRollback(
                root, 2, "Name", WorkType.GENERAL, "{}", true,
                "버전 1로 롤백", "user", "User", now, 1);

        assertThat(rollback.getChangeAction()).isEqualTo(ChangeAction.ROLLBACK);
        assertThat(rollback.getRollbackFromVersion()).isEqualTo(1);
        assertThat(rollback.getStatus()).isEqualTo(VersionStatus.PUBLISHED);
    }
}
