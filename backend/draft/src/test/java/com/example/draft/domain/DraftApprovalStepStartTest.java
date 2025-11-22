package com.example.draft.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftApprovalStepStartTest {

    @Test
    @DisplayName("WAITING 상태에서 start 호출 시 IN_PROGRESS로 전환된다")
    void startChangesStateToInProgress() {
        Draft draft = Draft.create("t", "c", "F", "ORG", "TPL", "creator", OffsetDateTime.now());
        DraftApprovalStep step = DraftApprovalStep.fromTemplate(new ApprovalTemplateStep(null, 1, "GRP", ""));
        draft.addApprovalStep(step);

        step.start(OffsetDateTime.now());

        assertThat(step.getState()).isEqualTo(DraftApprovalState.IN_PROGRESS);
    }
}
