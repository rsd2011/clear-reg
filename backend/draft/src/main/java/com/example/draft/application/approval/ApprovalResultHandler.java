package com.example.draft.application.approval;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.approval.api.ApprovalStatus;
import com.example.draft.application.audit.DraftAuditEvent;
import com.example.draft.application.audit.DraftAuditPublisher;
import com.example.draft.application.notification.DraftNotificationService;
import com.example.draft.application.port.out.ApprovalResultPort;
import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftAction;
import com.example.draft.domain.exception.DraftNotFoundException;
import com.example.draft.domain.repository.DraftRepository;

@Component
public class ApprovalResultHandler implements ApprovalResultPort {

    private final DraftRepository draftRepository;
    private final DraftAuditPublisher auditPublisher;
    private final DraftNotificationService notificationService;
    private final Clock clock;

    public ApprovalResultHandler(DraftRepository draftRepository,
                                 DraftAuditPublisher auditPublisher,
                                 DraftNotificationService notificationService,
                                 Clock clock) {
        this.draftRepository = draftRepository;
        this.auditPublisher = auditPublisher;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void onApprovalCompleted(UUID draftId, UUID approvalRequestId, ApprovalStatus status, String actor, String comment) {
        Draft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new DraftNotFoundException("기안을 찾을 수 없습니다."));

        if (draft.getApprovalRequestId() != null && !draft.getApprovalRequestId().equals(approvalRequestId)) {
            return; // 다른 요청이면 무시
        }
        draft.linkApprovalRequest(approvalRequestId);
        draft.applyApprovalResult(status, actor, comment, OffsetDateTime.now(clock));

        DraftAction action = mapStatusToAction(status);
        // 감사 로깅
        auditPublisher.publish(new DraftAuditEvent(action, draftId, actor,
                draft.getOrganizationCode(), comment, null, null, OffsetDateTime.now(clock)));
        // 알림 발송
        notificationService.notify("APPROVAL_RESULT", draft, actor, null, null, comment, OffsetDateTime.now(clock));
    }

    private DraftAction mapStatusToAction(ApprovalStatus status) {
        return switch (status) {
            case APPROVED -> DraftAction.APPROVED;
            case APPROVED_WITH_DEFER -> DraftAction.APPROVED_WITH_DEFER;
            case REJECTED -> DraftAction.REJECTED;
            case WITHDRAWN -> DraftAction.WITHDRAWN;
            default -> DraftAction.SUBMITTED;
        };
    }
}
