package com.example.draft.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import com.example.admin.approval.domain.ApprovalGroup;
import com.example.admin.approval.domain.ApprovalLineTemplate;
import com.example.admin.approval.domain.ApprovalTemplateStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftApprovalStepApproveSetsActorTest {

    @Test
    @DisplayName("approve 호출 시 actedBy와 actedAt이 설정된다")
    void approveSetsActorAndTime() {
        Draft draft = Draft.create("t", "c", "F", "ORG", "TPL", "creator", OffsetDateTime.now());

        OffsetDateTime now = OffsetDateTime.now();
        ApprovalGroup group = ApprovalGroup.create("GRP", "그룹", "설명", 1, now);
        ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿", 0, null, now);
        ApprovalTemplateStep templateStep = new ApprovalTemplateStep(template, 1, group);

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
