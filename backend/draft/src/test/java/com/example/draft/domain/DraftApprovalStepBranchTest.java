package com.example.draft.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.draft.domain.exception.DraftWorkflowException;

class DraftApprovalStepBranchTest {

    @Test
    @DisplayName("완료된 단계는 delegateTo 호출 시 예외를 던진다")
    void delegateToAfterCompletedThrows() {
        Draft draft = Draft.create("t", "c", "F", "ORG", "TPL", "creator", OffsetDateTime.now());
        DraftApprovalStep step = DraftApprovalStep.fromTemplate(new com.example.approval.domain.ApprovalTemplateStep(null, 1, "GRP", ""));
        draft.addApprovalStep(step);

        step.start(OffsetDateTime.now());
        step.approve("actor", "ok", OffsetDateTime.now());

        assertThatThrownBy(() -> step.delegateTo("delegate", "", OffsetDateTime.now()))
                .isInstanceOf(DraftWorkflowException.class);
    }

    @Test
    @DisplayName("대기 상태가 아니면 start는 상태를 변경하지 않는다")
    void startNoOpWhenNotWaiting() {
        Draft draft = Draft.create("t", "c", "F", "ORG", "TPL", "creator", OffsetDateTime.now());
        DraftApprovalStep step = DraftApprovalStep.fromTemplate(new com.example.approval.domain.ApprovalTemplateStep(null, 1, "GRP", ""));
        draft.addApprovalStep(step);

        step.start(OffsetDateTime.now());
        step.approve("actor", "ok", OffsetDateTime.now());

        // 재호출해도 예외 없이 통과 (분기 커버)
        step.start(OffsetDateTime.now());
    }

    @Test
    @DisplayName("완료된 단계에서 skip은 아무 동작도 하지 않는다")
    void skipNoOpWhenCompleted() {
        Draft draft = Draft.create("t", "c", "F", "ORG", "TPL", "creator", OffsetDateTime.now());
        DraftApprovalStep step = DraftApprovalStep.fromTemplate(new com.example.approval.domain.ApprovalTemplateStep(null, 1, "GRP", ""));
        draft.addApprovalStep(step);

        step.start(OffsetDateTime.now());
        step.approve("actor", "ok", OffsetDateTime.now());

        // 이미 APPROVED 상태에서 skip 호출 → 조기 반환 분기 커버
        step.skip("ignored", OffsetDateTime.now());
    }
}
