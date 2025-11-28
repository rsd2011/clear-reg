package com.example.draft.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import com.example.admin.approval.domain.ApprovalGroup;
import com.example.admin.approval.domain.ApprovalLineTemplate;
import com.example.admin.approval.domain.ApprovalTemplateStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftApprovalStepSkipTest {

    @Test
    @DisplayName("대기 상태에서는 skip 호출 시 상태가 SKIPPED로 변경된다")
    void skipChangesStateFromWaiting() {
        Draft draft = Draft.create("t", "c", "F", "ORG", "TPL", "creator", OffsetDateTime.now());

        OffsetDateTime now = OffsetDateTime.now();
        ApprovalGroup group = ApprovalGroup.create("GRP", "그룹", "설명", 1, now);
        ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿", 0, null, now);
        ApprovalTemplateStep templateStep = new ApprovalTemplateStep(template, 1, group);

        DraftApprovalStep step = DraftApprovalStep.fromTemplate(templateStep);
        draft.addApprovalStep(step);

        step.skip("no approver", now);

        assertThat(step.getState()).isEqualTo(DraftApprovalState.SKIPPED);
        assertThat(step.getComment()).isEqualTo("no approver");
        assertThat(step.getActedAt()).isEqualTo(now);
    }
}
