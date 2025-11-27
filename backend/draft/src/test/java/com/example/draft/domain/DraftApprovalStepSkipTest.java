package com.example.draft.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftApprovalStepSkipTest {

    @Test
    @DisplayName("대기 상태에서는 skip 호출 시 상태가 SKIPPED로 변경된다")
    void skipChangesStateFromWaiting() {
        Draft draft = Draft.create("t", "c", "F", "ORG", "TPL", "creator", OffsetDateTime.now());
        DraftApprovalStep step = DraftApprovalStep.fromTemplate(new com.example.admin.approval.ApprovalTemplateStep(null, 1, "GRP", ""));
        draft.addApprovalStep(step);

        OffsetDateTime now = OffsetDateTime.now();
        step.skip("no approver", now);

        assertThat(step.getState()).isEqualTo(DraftApprovalState.SKIPPED);
        assertThat(step.getComment()).isEqualTo("no approver");
        assertThat(step.getActedAt()).isEqualTo(now);
    }
}
