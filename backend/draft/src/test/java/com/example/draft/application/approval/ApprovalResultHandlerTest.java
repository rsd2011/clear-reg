package com.example.draft.application.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.example.approval.api.ApprovalStatus;
import com.example.draft.domain.Draft;
import com.example.draft.domain.repository.DraftRepository;
import com.example.draft.application.audit.DraftAuditPublisher;
import com.example.draft.application.notification.DraftNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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

    @Test
    void updatesDraftStatusOnApproved() {
        Draft draft = Draft.create("t", "c", "B", "ORG", "TPL", "creator", OffsetDateTime.now(clock));
        given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));

        handler.onApprovalCompleted(draft.getId(), UUID.randomUUID(), ApprovalStatus.APPROVED, "approver", "ok");

        assertThat(draft.getStatus()).isEqualTo(com.example.draft.domain.DraftStatus.APPROVED);
        verify(draftRepository).findById(draft.getId());
    }
}
