package com.example.approval;

import com.example.approval.api.ApprovalAction;
import com.example.approval.api.ApprovalActionCommand;
import com.example.approval.api.ApprovalRequestCommand;
import com.example.approval.api.ApprovalStatus;
import com.example.approval.api.ApprovalStatusSnapshot;
import com.example.approval.application.ApprovalAuthorizationService;
import com.example.approval.infra.memory.InMemoryApprovalFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
                requested.approvalRequestId(), new ApprovalActionCommand(ApprovalAction.APPROVE, "approver", "ORG", "ok", null)
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
                requested.approvalRequestId(), new ApprovalActionCommand(ApprovalAction.REJECT, "approver", "ORG", "no", null)
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
        approvalFacade.actOnApproval(requested.approvalRequestId(), new ApprovalActionCommand(ApprovalAction.APPROVE, "approver", "ORG", "ok", null));

        assertThatThrownBy(() -> approvalFacade.actOnApproval(requested.approvalRequestId(),
                        new ApprovalActionCommand(ApprovalAction.APPROVE, "approver", "ORG", "again", null)))
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
        assertThatThrownBy(() -> new ApprovalActionCommand(null, "actor", "ORG", "", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApprovalActionCommand(ApprovalAction.APPROVE, "", "ORG", "", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApprovalActionCommand(ApprovalAction.APPROVE, "actor", "", "", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApprovalActionCommand(ApprovalAction.APPROVE, "actor", "  ", "", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unknownApprovalIdThrows() {
        UUID missing = UUID.randomUUID();
        assertThatThrownBy(() -> approvalFacade.actOnApproval(missing, new ApprovalActionCommand(ApprovalAction.APPROVE, "a", "ORG", "", null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void authorizationServiceIsInvokedWhenPresent() {
        ApprovalAuthorizationService auth = Mockito.mock(ApprovalAuthorizationService.class);
        InMemoryApprovalFacade secured = new InMemoryApprovalFacade(auth);
        ApprovalStatusSnapshot requested = secured.requestApproval(
                new ApprovalRequestCommand(UUID.randomUUID(), "TMP", "ORG", "user", "summary", List.of("GRP1"))
        );

        secured.actOnApproval(requested.approvalRequestId(), new ApprovalActionCommand(ApprovalAction.APPROVE, "approver", "ORG", "", null));

        Mockito.verify(auth).ensureAuthorized(Mockito.any(), Mockito.eq(ApprovalAction.APPROVE), Mockito.eq("approver"), Mockito.eq("ORG"));
    }

    @Test
    void approveFirstStepInMultiStepFlowSetsInProgress() {
        UUID draftId = UUID.randomUUID();
        ApprovalStatusSnapshot requested = approvalFacade.requestApproval(
                new ApprovalRequestCommand(draftId, "TMP", "ORG", "user", "summary", List.of("GRP1", "GRP2"))
        );

        ApprovalStatusSnapshot afterFirst = approvalFacade.actOnApproval(requested.approvalRequestId(),
                new ApprovalActionCommand(ApprovalAction.APPROVE, "approver1", "ORG", "ok", null));

        assertThat(afterFirst.status()).isEqualTo(ApprovalStatus.IN_PROGRESS);
        assertThat(afterFirst.steps().get(0).status()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(afterFirst.steps().get(1).status()).isEqualTo(ApprovalStatus.REQUESTED);
    }

    @Test
    void deferSingleStepMovesToApprovedWithDefer() {
        ApprovalStatusSnapshot requested = approvalFacade.requestApproval(
                new ApprovalRequestCommand(UUID.randomUUID(), "TMP", "ORG", "user", "summary", List.of("GRP1"))
        );

        ApprovalStatusSnapshot deferred = approvalFacade.actOnApproval(
                requested.approvalRequestId(), new ApprovalActionCommand(ApprovalAction.DEFER, "approver", "ORG", "later", null)
        );

        assertThat(deferred.status()).isEqualTo(ApprovalStatus.APPROVED_WITH_DEFER);
        assertThat(deferred.steps()).singleElement().satisfies(step ->
                assertThat(step.status()).isEqualTo(ApprovalStatus.DEFERRED));

        ApprovalStatusSnapshot completed = approvalFacade.actOnApproval(
                requested.approvalRequestId(), new ApprovalActionCommand(ApprovalAction.DEFER_APPROVE, "approver", "ORG", "done", null)
        );

        assertThat(completed.status()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(completed.steps()).singleElement().satisfies(step ->
                assertThat(step.status()).isEqualTo(ApprovalStatus.APPROVED));
    }

    @Test
    void deferThenApproveNextStepLeavesApprovedWithDeferUntilCleared() {
        ApprovalStatusSnapshot requested = approvalFacade.requestApproval(
                new ApprovalRequestCommand(UUID.randomUUID(), "TMP", "ORG", "user", "summary", List.of("GRP1", "GRP2"))
        );

        approvalFacade.actOnApproval(requested.approvalRequestId(), new ApprovalActionCommand(ApprovalAction.DEFER, "approver1", "ORG", "out", null));
        ApprovalStatusSnapshot afterSecond = approvalFacade.actOnApproval(requested.approvalRequestId(), new ApprovalActionCommand(ApprovalAction.APPROVE, "approver2", "ORG", "ok", null));

        assertThat(afterSecond.status()).isEqualTo(ApprovalStatus.APPROVED_WITH_DEFER);
        assertThat(afterSecond.steps().get(0).status()).isEqualTo(ApprovalStatus.DEFERRED);
        assertThat(afterSecond.steps().get(1).status()).isEqualTo(ApprovalStatus.APPROVED);

        ApprovalStatusSnapshot finalApproved = approvalFacade.actOnApproval(requested.approvalRequestId(),
                new ApprovalActionCommand(ApprovalAction.DEFER_APPROVE, "approver1", "ORG", "done", null));

        assertThat(finalApproved.status()).isEqualTo(ApprovalStatus.APPROVED);
    }

    @Test
    void deferWithRemainingWaitingStepsSetsDeferredStatus() {
        ApprovalStatusSnapshot requested = approvalFacade.requestApproval(
                new ApprovalRequestCommand(UUID.randomUUID(), "TMP", "ORG", "user", "summary", List.of("GRP1", "GRP2"))
        );

        ApprovalStatusSnapshot deferred = approvalFacade.actOnApproval(requested.approvalRequestId(),
                new ApprovalActionCommand(ApprovalAction.DEFER, "approver1", "ORG", "later", null));

        assertThat(deferred.status()).isEqualTo(ApprovalStatus.DEFERRED);
    }

    @Test
    void withdrawBeforeCompletionSetsWithdrawn() {
        ApprovalStatusSnapshot requested = approvalFacade.requestApproval(
                new ApprovalRequestCommand(UUID.randomUUID(), "TMP", "ORG", "user", "summary", List.of("GRP1"))
        );

        ApprovalStatusSnapshot withdrawn = approvalFacade.actOnApproval(
                requested.approvalRequestId(), new ApprovalActionCommand(ApprovalAction.WITHDRAW, "requester", "ORG", "cancel", null)
        );

        assertThat(withdrawn.status()).isEqualTo(ApprovalStatus.WITHDRAWN);
        assertThatThrownBy(() -> approvalFacade.actOnApproval(requested.approvalRequestId(),
                new ApprovalActionCommand(ApprovalAction.APPROVE, "approver", "ORG", "", null))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void withdrawAfterApprovedNotAllowed() {
        ApprovalStatusSnapshot requested = approvalFacade.requestApproval(
                new ApprovalRequestCommand(UUID.randomUUID(), "TMP", "ORG", "user", "summary", List.of("GRP1"))
        );
        approvalFacade.actOnApproval(requested.approvalRequestId(), new ApprovalActionCommand(ApprovalAction.APPROVE, "approver", "ORG", "ok", null));

        assertThatThrownBy(() -> approvalFacade.actOnApproval(requested.approvalRequestId(),
                new ApprovalActionCommand(ApprovalAction.WITHDRAW, "approver", "ORG", "cancel", null)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void withdrawAfterApprovedWithDeferNotAllowed() {
        ApprovalStatusSnapshot requested = approvalFacade.requestApproval(
                new ApprovalRequestCommand(UUID.randomUUID(), "TMP", "ORG", "user", "summary", List.of("GRP1", "GRP2"))
        );
        approvalFacade.actOnApproval(requested.approvalRequestId(), new ApprovalActionCommand(ApprovalAction.DEFER, "approver1", "ORG", "later", null));
        approvalFacade.actOnApproval(requested.approvalRequestId(), new ApprovalActionCommand(ApprovalAction.APPROVE, "approver2", "ORG", "ok", null));

        assertThatThrownBy(() -> approvalFacade.actOnApproval(requested.approvalRequestId(),
                new ApprovalActionCommand(ApprovalAction.WITHDRAW, "approver1", "ORG", "cancel", null)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void delegateRecordsDelegatedTo() {
        ApprovalStatusSnapshot requested = approvalFacade.requestApproval(
                new ApprovalRequestCommand(UUID.randomUUID(), "TMP", "ORG", "user", "summary", List.of("GRP1"))
        );

        ApprovalStatusSnapshot delegated = approvalFacade.actOnApproval(
                requested.approvalRequestId(), new ApprovalActionCommand(ApprovalAction.DELEGATE, "approver", "ORG", "pls", "delegatee")
        );

        assertThat(delegated.steps()).singleElement().satisfies(step -> {
            assertThat(step.delegatedTo()).isEqualTo("delegatee");
            assertThat(step.status()).isEqualTo(ApprovalStatus.REQUESTED);
        });
    }
}
