package com.example.draft.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.example.admin.approval.ApprovalLineTemplate;
import com.example.draft.domain.exception.DraftAccessDeniedException;
import com.example.draft.domain.exception.DraftWorkflowException;

class DraftDomainTest {

    private static final OffsetDateTime NOW = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void givenTwoStepWorkflow_whenApproveBoth_thenStatusApproved() {
        Draft draft = createDraft();
        draft.submit("writer", NOW.plusMinutes(1));

        var firstStep = draft.getApprovalSteps().get(0);
        draft.approveStep(firstStep.getId(), "approver1", "", NOW.plusMinutes(2));

        var secondStep = draft.getApprovalSteps().get(1);
        draft.approveStep(secondStep.getId(), "approver2", "", NOW.plusMinutes(3));

        assertThat(draft.getStatus()).isEqualTo(DraftStatus.APPROVED);
        assertThat(draft.getCompletedAt()).isNotNull();
    }

    @Test
    void givenWorkflow_whenRejected_thenStatusRejectedAndStepsSkipped() {
        Draft draft = createDraft();
        draft.submit("writer", NOW.plusMinutes(1));

        var firstStep = draft.getApprovalSteps().get(0);
        draft.rejectStep(firstStep.getId(), "approver", "사유", NOW.plusMinutes(2));

        assertThat(draft.getStatus()).isEqualTo(DraftStatus.REJECTED);
        assertThat(draft.getApprovalSteps()).allMatch(step -> step.getState().isCompleted());
    }

    @Test
    void givenDifferentOrganization_whenNoAudit_thenAccessDenied() {
        Draft draft = createDraft();

        assertThatThrownBy(() -> draft.assertOrganizationAccess("OTHER", false))
                .isInstanceOf(DraftAccessDeniedException.class);

        draft.assertOrganizationAccess("OTHER", true); // 감사 접근은 허용
    }

    @Test
    void withdrawBlockedWhenApprovedWithDefer() {
        Draft draft = createDraft();
        draft.submit("writer", NOW.plusMinutes(1));
        var firstStep = draft.getApprovalSteps().get(0);
        draft.deferStep(firstStep.getId(), "approver1", "후결", NOW.plusMinutes(2));
        var secondStep = draft.getApprovalSteps().get(1);
        draft.approveStep(secondStep.getId(), "approver2", "ok", NOW.plusMinutes(3));

        assertThat(draft.getStatus()).isEqualTo(DraftStatus.APPROVED_WITH_DEFER);
        assertThatThrownBy(() -> draft.withdraw("writer", NOW.plusMinutes(4)))
                .isInstanceOf(DraftWorkflowException.class);
    }

    @Test
    void applyApprovalResultUpdatesNewStatuses() {
        Draft draft = createDraft();
        draft.applyApprovalResult(com.example.approval.api.ApprovalStatus.APPROVED_WITH_DEFER, "actor", "", NOW.plusMinutes(1));
        assertThat(draft.getStatus()).isEqualTo(DraftStatus.APPROVED_WITH_DEFER);

        draft.applyApprovalResult(com.example.approval.api.ApprovalStatus.WITHDRAWN, "actor", "", NOW.plusMinutes(2));
        assertThat(draft.getStatus()).isEqualTo(DraftStatus.WITHDRAWN);
    }

    @Test
    void givenDeferredStep_whenSubsequentApproved_thenStatusApprovedWithDeferUntilCleared() {
        Draft draft = createDraft();
        draft.submit("writer", NOW.plusMinutes(1));

        var firstStep = draft.getApprovalSteps().get(0);
        draft.deferStep(firstStep.getId(), "approver1", "후결", NOW.plusMinutes(2));

        var secondStep = draft.getApprovalSteps().get(1);
        draft.approveStep(secondStep.getId(), "approver2", "ok", NOW.plusMinutes(3));

        assertThat(draft.getStatus()).isEqualTo(DraftStatus.APPROVED_WITH_DEFER);

        draft.approveDeferredStep(firstStep.getId(), "approver1", "마감", NOW.plusMinutes(4));
        assertThat(draft.getStatus()).isEqualTo(DraftStatus.APPROVED);
    }

    private Draft createDraft() {
        ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿", "NOTICE", "ORG-001", NOW);
        template.addStep(1, "GROUP-A", "첫 번째");
        template.addStep(2, "GROUP-B", "두 번째");
        Draft draft = Draft.create("제목", "내용", "NOTICE", "ORG-001",
                template.getTemplateCode(), "writer", NOW);
        template.getSteps().stream()
                .map(DraftApprovalStep::fromTemplate)
                .forEach(draft::addApprovalStep);
        draft.initializeWorkflow(NOW);
        return draft;
    }
}
