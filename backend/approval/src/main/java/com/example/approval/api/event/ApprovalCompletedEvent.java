package com.example.approval.api.event;

import java.util.UUID;

import com.example.approval.api.ApprovalStatus;

/**
 * Approval 모듈이 Draft에 결과를 전달할 때 사용.
 */
public record ApprovalCompletedEvent(
        UUID approvalRequestId,
        UUID draftId,
        ApprovalStatus status,
        String actedBy,
        String comment
) {
}
