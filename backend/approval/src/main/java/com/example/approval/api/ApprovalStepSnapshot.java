package com.example.approval.api;

import java.time.OffsetDateTime;

public record ApprovalStepSnapshot(
        int stepOrder,
        String approvalGroupCode,
        ApprovalStatus status,
        String actedBy,
        OffsetDateTime actedAt
) {
}
