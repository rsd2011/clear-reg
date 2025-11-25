package com.example.approval.api;

import java.util.UUID;

public interface ApprovalFacade {

    ApprovalStatusSnapshot requestApproval(ApprovalRequestCommand command);

    ApprovalStatusSnapshot actOnApproval(UUID approvalRequestId, ApprovalActionCommand command);

    ApprovalStatusSnapshot findByDraftId(UUID draftId);
}
