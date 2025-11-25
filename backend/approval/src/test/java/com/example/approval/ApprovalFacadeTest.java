package com.example.approval;

import com.example.approval.api.ApprovalAction;
import com.example.approval.api.ApprovalActionCommand;
import com.example.approval.api.ApprovalRequestCommand;
import com.example.approval.api.ApprovalStatus;
import com.example.approval.api.ApprovalStatusSnapshot;
import com.example.approval.infra.memory.InMemoryApprovalFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApprovalFacadeTest {

    private InMemoryApprovalFacade approvalFacade;

    @BeforeEach
    void setUp() {
        approvalFacade = new InMemoryApprovalFacade();
    }

    @Test
    void requestApprovalCreatesPendingRequest() {
        UUID draftId = UUID.randomUUID();
        ApprovalStatusSnapshot snapshot = approvalFacade.requestApproval(
                new ApprovalRequestCommand(draftId, "TMP", "ORG", "user", "summary", List.of("GRP1", "GRP2"))
        );

        assertThat(snapshot.draftId()).isEqualTo(draftId);
        assertThat(snapshot.status()).isEqualTo(ApprovalStatus.REQUESTED);
        assertThat(snapshot.steps()).hasSize(2);
        assertThat(snapshot.steps().get(0).approvalGroupCode()).isEqualTo("GRP1");
    }

    @Test
    void approveSingleStepCompletesRequest() {
        ApprovalStatusSnapshot requested = approvalFacade.requestApproval(
                new ApprovalRequestCommand(UUID.randomUUID(), "TMP", "ORG", "user", "summary", List.of("GRP1"))
        );

        ApprovalStatusSnapshot approved = approvalFacade.actOnApproval(
                requested.approvalRequestId(), new ApprovalActionCommand(ApprovalAction.APPROVE, "approver", "ok")
        );

        assertThat(approved.status()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(approved.steps()).singleElement().satisfies(step -> {
            assertThat(step.status()).isEqualTo(ApprovalStatus.APPROVED);
            assertThat(step.actedBy()).isEqualTo("approver");
            assertThat(step.actedAt()).isNotNull();
        });
    }

    @Test
    void rejectSetsRejectedStatus() {
        ApprovalStatusSnapshot requested = approvalFacade.requestApproval(
                new ApprovalRequestCommand(UUID.randomUUID(), "TMP", "ORG", "user", "summary", List.of("GRP1"))
        );

        ApprovalStatusSnapshot rejected = approvalFacade.actOnApproval(
                requested.approvalRequestId(), new ApprovalActionCommand(ApprovalAction.REJECT, "approver", "no")
        );

        assertThat(rejected.status()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(rejected.steps()).singleElement().satisfies(step ->
                assertThat(step.status()).isEqualTo(ApprovalStatus.REJECTED));
    }

    @Test
    void actionAfterCompletionThrows() {
        ApprovalStatusSnapshot requested = approvalFacade.requestApproval(
                new ApprovalRequestCommand(UUID.randomUUID(), "TMP", "ORG", "user", "summary", List.of("GRP1"))
        );
        approvalFacade.actOnApproval(requested.approvalRequestId(), new ApprovalActionCommand(ApprovalAction.APPROVE, "approver", "ok"));

        assertThatThrownBy(() -> approvalFacade.actOnApproval(requested.approvalRequestId(),
                        new ApprovalActionCommand(ApprovalAction.APPROVE, "approver", "again")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void findByDraftIdReturnsSnapshot() {
        UUID draftId = UUID.randomUUID();
        ApprovalStatusSnapshot requested = approvalFacade.requestApproval(
                new ApprovalRequestCommand(draftId, "TMP", "ORG", "user", "summary", List.of("GRP1"))
        );

        ApprovalStatusSnapshot found = approvalFacade.findByDraftId(draftId);
        assertThat(found).isNotNull();
        assertThat(found.approvalRequestId()).isEqualTo(requested.approvalRequestId());
    }

    @Test
    void findByDraftIdReturnsNullWhenNotExists() {
        assertThat(approvalFacade.findByDraftId(UUID.randomUUID())).isNull();
    }

    @Test
    void requestApprovalValidatesMandatoryFields() {
        UUID draftId = UUID.randomUUID();
        assertThatThrownBy(() -> new ApprovalRequestCommand(draftId, "TMP", "ORG", "user", "summary", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApprovalRequestCommand(draftId, "TMP", "ORG", "", "summary", List.of("G")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApprovalRequestCommand(draftId, "", "ORG", "user", "summary", List.of("G")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApprovalRequestCommand(draftId, "TMP", "", "user", "summary", List.of("G")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void actionCommandValidatesMandatoryFields() {
        assertThatThrownBy(() -> new ApprovalActionCommand(null, "actor", ""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApprovalActionCommand(ApprovalAction.APPROVE, "", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unknownApprovalIdThrows() {
        UUID missing = UUID.randomUUID();
        assertThatThrownBy(() -> approvalFacade.actOnApproval(missing, new ApprovalActionCommand(ApprovalAction.APPROVE, "a", "")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void approveFirstStepInMultiStepFlowSetsInProgress() {
        UUID draftId = UUID.randomUUID();
        ApprovalStatusSnapshot requested = approvalFacade.requestApproval(
                new ApprovalRequestCommand(draftId, "TMP", "ORG", "user", "summary", List.of("GRP1", "GRP2"))
        );

        ApprovalStatusSnapshot afterFirst = approvalFacade.actOnApproval(requested.approvalRequestId(),
                new ApprovalActionCommand(ApprovalAction.APPROVE, "approver1", "ok"));

        assertThat(afterFirst.status()).isEqualTo(ApprovalStatus.IN_PROGRESS);
        assertThat(afterFirst.steps().get(0).status()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(afterFirst.steps().get(1).status()).isEqualTo(ApprovalStatus.REQUESTED);
    }
}
