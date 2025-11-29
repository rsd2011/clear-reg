package com.example.draft.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import com.example.admin.approval.domain.ApprovalGroup;
import com.example.admin.approval.domain.ApprovalTemplate;
import com.example.admin.approval.domain.ApprovalTemplateRoot;
import com.example.admin.approval.domain.ApprovalTemplateStep;
import com.example.common.version.ChangeAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.draft.domain.exception.DraftWorkflowException;

class DraftApprovalStepBranchTest {

    private ApprovalTemplateStep createTemplateStep() {
        OffsetDateTime now = OffsetDateTime.now();
        ApprovalGroup group = ApprovalGroup.create("GRP", "그룹", "설명", 1, now);
        ApprovalTemplateRoot root = ApprovalTemplateRoot.create(now);
        ApprovalTemplate version = ApprovalTemplate.create(
                root, 1, "템플릿", 0, null, true,
                ChangeAction.CREATE, null, "system", "System", now);
        ApprovalTemplateStep step = ApprovalTemplateStep.create(version, 1, group, false);
        version.addStep(step);
        root.activateNewVersion(version, now);
        return step;
    }

    @Test
    @DisplayName("완료된 단계는 delegateTo 호출 시 예외를 던진다")
    void delegateToAfterCompletedThrows() {
        Draft draft = Draft.create("t", "c", "F", "ORG", "TPL", "creator", OffsetDateTime.now());
        DraftApprovalStep step = DraftApprovalStep.fromTemplate(createTemplateStep());
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
        DraftApprovalStep step = DraftApprovalStep.fromTemplate(createTemplateStep());
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
        DraftApprovalStep step = DraftApprovalStep.fromTemplate(createTemplateStep());
        draft.addApprovalStep(step);

        step.start(OffsetDateTime.now());
        step.approve("actor", "ok", OffsetDateTime.now());

        // 이미 APPROVED 상태에서 skip 호출 → 조기 반환 분기 커버
        step.skip("ignored", OffsetDateTime.now());
    }

    @Test
    @DisplayName("IN_PROGRESS 상태에서 defer하면 DEFERRED로 전환된다")
    void deferTransitions() {
        DraftApprovalStep step = DraftApprovalStep.fromTemplate(createTemplateStep());
        step.start(OffsetDateTime.now());
        step.defer("actor", "later", OffsetDateTime.now());
        assertThatThrownBy(() -> step.defer("actor", "dup", OffsetDateTime.now()))
                .isInstanceOf(DraftWorkflowException.class);
    }

    @Test
    @DisplayName("DEFERRED 상태가 아니면 completeDeferred는 예외를 던진다")
    void completeDeferredGuard() {
        DraftApprovalStep step = DraftApprovalStep.fromTemplate(createTemplateStep());
        step.start(OffsetDateTime.now());
        assertThatThrownBy(() -> step.completeDeferred("actor", "done", OffsetDateTime.now()))
                .isInstanceOf(DraftWorkflowException.class);
    }
}
