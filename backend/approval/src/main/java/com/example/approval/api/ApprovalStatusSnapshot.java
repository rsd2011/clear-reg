package com.example.approval.api;

import java.util.List;
import java.util.UUID;

public record ApprovalStatusSnapshot(
        UUID approvalRequestId,
        UUID draftId,
        ApprovalStatus status,
        List<ApprovalStepSnapshot> steps
) {
}
