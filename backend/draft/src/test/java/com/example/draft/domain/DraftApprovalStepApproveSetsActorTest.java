package com.example.draft.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftApprovalStepApproveSetsActorTest {

    @Test
    @DisplayName("approve 호출 시 actedBy와 actedAt이 설정된다")
    void approveSetsActorAndTime() {
        Draft draft = Draft.create("t", "c", "F", "ORG", "TPL", "creator", OffsetDateTime.now());
        DraftApprovalStep step = DraftApprovalStep.fromTemplate(new ApprovalTemplateStep(null, 1, "GRP", ""));
        draft.addApprovalStep(step);

        OffsetDateTime now = OffsetDateTime.now();
        step.start(now);
        step.approve("approver", "ok", now);

        assertThat(step.getActedBy()).isEqualTo("approver");
        assertThat(step.getActedAt()).isEqualTo(now);
        assertThat(step.getComment()).isEqualTo("ok");
        assertThat(step.getState()).isEqualTo(DraftApprovalState.APPROVED);
    }
}
