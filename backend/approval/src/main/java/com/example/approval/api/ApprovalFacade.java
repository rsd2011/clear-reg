package com.example.approval.api;

import java.util.UUID;

import com.example.approval.api.dto.ApprovalActionCommand;
import com.example.approval.api.dto.ApprovalRequestCommand;

public interface ApprovalFacade {

    ApprovalStatusSnapshot requestApproval(ApprovalRequestCommand command);

    ApprovalStatusSnapshot actOnApproval(UUID approvalRequestId, ApprovalActionCommand command);

    ApprovalStatusSnapshot findByDraftId(UUID draftId);
}
