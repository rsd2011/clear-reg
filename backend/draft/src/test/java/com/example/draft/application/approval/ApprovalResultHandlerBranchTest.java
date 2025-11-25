package com.example.draft.application.approval;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.approval.api.ApprovalStatus;
import com.example.draft.application.audit.DraftAuditPublisher;
import com.example.draft.application.notification.DraftNotificationService;
import com.example.draft.domain.Draft;
import com.example.draft.domain.exception.DraftNotFoundException;
import com.example.draft.domain.repository.DraftRepository;

class ApprovalResultHandlerBranchTest {

    private DraftRepository draftRepository;
    private DraftAuditPublisher auditPublisher;
    private DraftNotificationService notificationService;
    private ApprovalResultHandler handler;
    private final Clock clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        draftRepository = mock(DraftRepository.class);
        auditPublisher = mock(DraftAuditPublisher.class);
        notificationService = mock(DraftNotificationService.class);
        handler = new ApprovalResultHandler(draftRepository, auditPublisher, notificationService, clock);
    }

    @Test
    @DisplayName("승인 요청 ID가 다르면 아무 것도 하지 않는다")
    void ignoresMismatchedApprovalRequestId() {
        UUID draftId = UUID.randomUUID();
        UUID approvalId = UUID.randomUUID();
        Draft draft = Draft.create("t", "c", "B", "ORG", "TPL", "creator", java.time.OffsetDateTime.now(clock));
        draft.linkApprovalRequest(UUID.randomUUID());
        when(draftRepository.findById(draftId)).thenReturn(Optional.of(draft));

        handler.onApprovalCompleted(draftId, approvalId, ApprovalStatus.APPROVED, "actor", "ok");

        verify(auditPublisher, never()).publish(any());
        verify(notificationService, never()).notify(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("기안을 찾지 못하면 DraftNotFoundException")
    void throwsWhenDraftMissing() {
        UUID draftId = UUID.randomUUID();
        when(draftRepository.findById(draftId)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> handler.onApprovalCompleted(draftId, UUID.randomUUID(), ApprovalStatus.APPROVED, "actor", null))
                .isInstanceOf(DraftNotFoundException.class);
    }
}
