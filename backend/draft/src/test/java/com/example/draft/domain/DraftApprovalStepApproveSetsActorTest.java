package com.example.draft.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import com.example.admin.approval.domain.ApprovalGroup;
import com.example.admin.approval.domain.ApprovalTemplate;
import com.example.admin.approval.domain.ApprovalTemplateRoot;
import com.example.admin.approval.domain.ApprovalTemplateStep;
import com.example.common.version.ChangeAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftApprovalStepApproveSetsActorTest {

    @Test
    @DisplayName("approve 호출 시 actedBy와 actedAt이 설정된다")
    void approveSetsActorAndTime() {
        Draft draft = Draft.create("t", "c", "F", "ORG", "TPL", "creator", OffsetDateTime.now());

        OffsetDateTime now = OffsetDateTime.now();
        ApprovalGroup group = ApprovalGroup.create("GRP", "그룹", "설명", 1, now);
        ApprovalTemplateRoot root = ApprovalTemplateRoot.create(now);
        ApprovalTemplate version = ApprovalTemplate.create(
                root, 1, "템플릿", 0, null, true,
                ChangeAction.CREATE, null, "system", "System", now);
        ApprovalTemplateStep templateStep = ApprovalTemplateStep.create(version, 1, group, false);
        version.addStep(templateStep);
        root.activateNewVersion(version, now);

        DraftApprovalStep step = DraftApprovalStep.fromTemplate(templateStep);
        draft.addApprovalStep(step);

        step.start(now);
        step.approve("approver", "ok", now);

        assertThat(step.getActedBy()).isEqualTo("approver");
        assertThat(step.getActedAt()).isEqualTo(now);
        assertThat(step.getComment()).isEqualTo("ok");
        assertThat(step.getState()).isEqualTo(DraftApprovalState.APPROVED);
    }
}
