package com.example.draft.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.draft.domain.exception.DraftWorkflowException;

class DraftApprovalStepRejectGuardTest {

    @Test
    @DisplayName("IN_PROGRESS 상태가 아니면 reject 시 예외를 던진다")
    void rejectNotInProgressThrows() {
        Draft draft = Draft.create("t", "c", "F", "ORG", "TPL", "creator", OffsetDateTime.now());
        DraftApprovalStep step = DraftApprovalStep.fromTemplate(new ApprovalTemplateStep(null, 1, "GRP", ""));
        draft.addApprovalStep(step);

        assertThatThrownBy(() -> step.reject("actor", "comment", OffsetDateTime.now()))
                .isInstanceOf(DraftWorkflowException.class);
    }
}
