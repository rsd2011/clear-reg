package com.example.draft.application.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.example.approval.api.ApprovalStatus;
import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftStatus;
import com.example.draft.domain.repository.DraftRepository;
import com.example.draft.application.audit.DraftAuditPublisher;
import com.example.draft.application.notification.DraftNotificationService;
import com.example.draft.domain.exception.DraftNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@DisplayName("ApprovalResultHandler 테스트")
class ApprovalResultHandlerTest {

    @Mock
    DraftRepository draftRepository;

    @Mock
    DraftAuditPublisher auditPublisher;

    @Mock
    DraftNotificationService notificationService;

    private ApprovalResultHandler handler;
    private Clock clock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
        handler = new ApprovalResultHandler(draftRepository, auditPublisher, notificationService, clock);
    }

    @Nested
    @DisplayName("onApprovalCompleted 메서드")
    class OnApprovalCompleted {

        @Test
        @DisplayName("Given APPROVED 상태 When 호출하면 Then APPROVED 상태로 변경된다")
        void updatesDraftStatusOnApproved() {
            Draft draft = Draft.create("t", "c", "B", "ORG", "TPL", "creator", OffsetDateTime.now(clock));
            given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));

            handler.onApprovalCompleted(draft.getId(), UUID.randomUUID(), ApprovalStatus.APPROVED, "approver", "ok");

            assertThat(draft.getStatus()).isEqualTo(DraftStatus.APPROVED);
            verify(draftRepository).findById(draft.getId());
            verify(auditPublisher).publish(any());
            verify(notificationService).notify(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Given REJECTED 상태 When 호출하면 Then REJECTED 상태로 변경된다")
        void updatesDraftStatusOnRejected() {
            Draft draft = Draft.create("t", "c", "B", "ORG", "TPL", "creator", OffsetDateTime.now(clock));
            given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));

            handler.onApprovalCompleted(draft.getId(), UUID.randomUUID(), ApprovalStatus.REJECTED, "approver", "거절");

            assertThat(draft.getStatus()).isEqualTo(DraftStatus.REJECTED);
        }

        @Test
        @DisplayName("Given WITHDRAWN 상태 When 호출하면 Then WITHDRAWN 상태로 변경된다")
        void updatesDraftStatusOnWithdrawn() {
            Draft draft = Draft.create("t", "c", "B", "ORG", "TPL", "creator", OffsetDateTime.now(clock));
            given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));

            handler.onApprovalCompleted(draft.getId(), UUID.randomUUID(), ApprovalStatus.WITHDRAWN, "user", "철회");

            assertThat(draft.getStatus()).isEqualTo(DraftStatus.WITHDRAWN);
        }

        @Test
        @DisplayName("Given APPROVED_WITH_DEFER 상태 When 호출하면 Then APPROVED_WITH_DEFER 상태로 변경된다")
        void updatesDraftStatusOnApprovedWithDefer() {
            Draft draft = Draft.create("t", "c", "B", "ORG", "TPL", "creator", OffsetDateTime.now(clock));
            given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));

            handler.onApprovalCompleted(draft.getId(), UUID.randomUUID(), ApprovalStatus.APPROVED_WITH_DEFER, "approver", "조건부 승인");

            assertThat(draft.getStatus()).isEqualTo(DraftStatus.APPROVED_WITH_DEFER);
        }

        @Test
        @DisplayName("Given REQUESTED 상태 When 호출하면 Then default로 SUBMITTED action 매핑된다")
        void defaultStatusMapsToSubmitted() {
            Draft draft = Draft.create("t", "c", "B", "ORG", "TPL", "creator", OffsetDateTime.now(clock));
            given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));

            handler.onApprovalCompleted(draft.getId(), UUID.randomUUID(), ApprovalStatus.REQUESTED, "user", "요청");

            // REQUESTED는 default case로 SUBMITTED action으로 매핑됨
            verify(auditPublisher).publish(any());
        }

        @Test
        @DisplayName("Given DEFERRED 상태 When 호출하면 Then default로 SUBMITTED action 매핑된다")
        void deferredStatusMapsToSubmitted() {
            Draft draft = Draft.create("t", "c", "B", "ORG", "TPL", "creator", OffsetDateTime.now(clock));
            given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));

            handler.onApprovalCompleted(draft.getId(), UUID.randomUUID(), ApprovalStatus.DEFERRED, "user", "보류");

            // DEFERRED도 default case로 SUBMITTED action으로 매핑됨
            verify(auditPublisher).publish(any());
        }

        @Test
        @DisplayName("Given IN_PROGRESS 상태 When 호출하면 Then default로 SUBMITTED action 매핑된다")
        void inProgressStatusMapsToSubmitted() {
            Draft draft = Draft.create("t", "c", "B", "ORG", "TPL", "creator", OffsetDateTime.now(clock));
            given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));

            handler.onApprovalCompleted(draft.getId(), UUID.randomUUID(), ApprovalStatus.IN_PROGRESS, "user", "진행중");

            // IN_PROGRESS도 default case로 SUBMITTED action으로 매핑됨
            verify(auditPublisher).publish(any());
        }

        @Test
        @DisplayName("Given 기안이 없을 때 When 호출하면 Then DraftNotFoundException 발생")
        void throwsExceptionWhenDraftNotFound() {
            UUID draftId = UUID.randomUUID();
            given(draftRepository.findById(draftId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> handler.onApprovalCompleted(draftId, UUID.randomUUID(), ApprovalStatus.APPROVED, "user", "ok"))
                    .isInstanceOf(DraftNotFoundException.class)
                    .hasMessageContaining("기안을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("Given 다른 approvalRequestId When 호출하면 Then 아무 처리 없이 반환")
        void ignoresWhenDifferentApprovalRequestId() {
            Draft draft = Draft.create("t", "c", "B", "ORG", "TPL", "creator", OffsetDateTime.now(clock));
            UUID existingRequestId = UUID.randomUUID();
            UUID differentRequestId = UUID.randomUUID();

            // 기안에 이미 다른 요청 ID가 연결되어 있음
            draft.linkApprovalRequest(existingRequestId);
            given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));

            DraftStatus originalStatus = draft.getStatus();

            handler.onApprovalCompleted(draft.getId(), differentRequestId, ApprovalStatus.APPROVED, "approver", "ok");

            // 상태가 변경되지 않아야 함
            assertThat(draft.getStatus()).isEqualTo(originalStatus);
            verify(auditPublisher, never()).publish(any());
            verify(notificationService, never()).notify(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Given 동일한 approvalRequestId When 호출하면 Then 정상 처리된다")
        void processesWhenSameApprovalRequestId() {
            Draft draft = Draft.create("t", "c", "B", "ORG", "TPL", "creator", OffsetDateTime.now(clock));
            UUID requestId = UUID.randomUUID();
            draft.linkApprovalRequest(requestId);
            given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));

            handler.onApprovalCompleted(draft.getId(), requestId, ApprovalStatus.APPROVED, "approver", "ok");

            assertThat(draft.getStatus()).isEqualTo(DraftStatus.APPROVED);
            verify(auditPublisher).publish(any());
        }

        @Test
        @DisplayName("Given null approvalRequestId가 기안에 없을 때 When 호출하면 Then 요청 ID가 연결된다")
        void linksApprovalRequestWhenNull() {
            Draft draft = Draft.create("t", "c", "B", "ORG", "TPL", "creator", OffsetDateTime.now(clock));
            UUID requestId = UUID.randomUUID();
            given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));

            assertThat(draft.getApprovalRequestId()).isNull();

            handler.onApprovalCompleted(draft.getId(), requestId, ApprovalStatus.APPROVED, "approver", "ok");

            assertThat(draft.getApprovalRequestId()).isEqualTo(requestId);
        }
    }
}
