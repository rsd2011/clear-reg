package com.example.approval;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import com.example.admin.approval.domain.ApprovalGroup;
import com.example.admin.approval.domain.ApprovalTemplate;
import com.example.admin.approval.domain.ApprovalTemplateRoot;
import com.example.admin.approval.domain.ApprovalTemplateStep;
import com.example.common.version.ChangeAction;
import org.junit.jupiter.api.Test;

class ApprovalDomainEntitiesTest {

    private static final OffsetDateTime NOW = OffsetDateTime.now();

    @Test
    void approvalGroupCreatesAndRenames() {
        ApprovalGroup group = ApprovalGroup.create("CODE", "이름", "desc", 0, NOW);
        group.rename("새 이름", "새 desc", NOW.plusSeconds(1));

        assertThat(group.getName()).isEqualTo("새 이름");
        assertThat(group.getUpdatedAt()).isAfter(NOW);
        assertThat(group.getDescription()).isEqualTo("새 desc");
    }

    @Test
    void approvalTemplateCreatesAndAddsSteps() {
        ApprovalTemplateRoot root = ApprovalTemplateRoot.create(NOW);
        ApprovalTemplate version = ApprovalTemplate.create(
                root, 1, "템플릿", 0, null, true,
                ChangeAction.CREATE, null, "user", "사용자", NOW);

        assertThat(version.getName()).isEqualTo("템플릿");
        assertThat(version.isActive()).isTrue();

        ApprovalGroup group1 = ApprovalGroup.create("GRP1", "그룹1", "desc", 1, NOW);
        ApprovalGroup group2 = ApprovalGroup.create("GRP2", "그룹2", "desc", 2, NOW);

        ApprovalTemplateStep step1 = ApprovalTemplateStep.create(version, 1, group1, false);
        ApprovalTemplateStep step2 = ApprovalTemplateStep.create(version, 2, group2, false);
        version.addStep(step1);
        version.addStep(step2);
        assertThat(version.getSteps()).hasSize(2);

        ApprovalGroup group3 = ApprovalGroup.create("GRP3", "그룹3", "desc", 3, NOW);
        ApprovalTemplateStep step3 = ApprovalTemplateStep.create(version, 3, group3, false);
        version.replaceSteps(java.util.List.of(step3));
        assertThat(version.getSteps()).hasSize(1);
        assertThat(version.getSteps().getFirst().getApprovalGroup().getGroupCode()).isEqualTo("GRP3");
    }

    @Test
    void approvalTemplateVersionUpdatesViaDraft() {
        ApprovalTemplateRoot root = ApprovalTemplateRoot.create(NOW);
        ApprovalTemplate draft = ApprovalTemplate.createDraft(
                root, 2, "초안", 0, null, true,
                null, "user", "사용자", NOW);

        assertThat(draft.isDraft()).isTrue();

        draft.updateDraft("수정된 초안", 1, "설명", false, "변경 사유", NOW.plusSeconds(5));

        assertThat(draft.getName()).isEqualTo("수정된 초안");
        assertThat(draft.isActive()).isFalse();
    }

    @Test
    void approvalTemplateStepStoresValues() {
        ApprovalTemplateRoot root = ApprovalTemplateRoot.create(NOW);
        ApprovalTemplate version = ApprovalTemplate.create(
                root, 1, "템플릿", 0, null, true,
                ChangeAction.CREATE, null, "user", "사용자", NOW);
        ApprovalGroup group = ApprovalGroup.create("GRP2", "그룹2", "설명", 2, NOW);
        var step = ApprovalTemplateStep.create(version, 2, group, false);
        assertThat(step.getTemplate()).isEqualTo(version);
        assertThat(step.getStepOrder()).isEqualTo(2);
        assertThat(step.getApprovalGroup().getGroupCode()).isEqualTo("GRP2");
    }
}
