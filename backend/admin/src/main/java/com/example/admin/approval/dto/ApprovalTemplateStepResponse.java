package com.example.admin.approval.dto;

import java.util.UUID;

import com.example.admin.approval.ApprovalTemplateStep;

public record ApprovalTemplateStepResponse(
        UUID id,
        int stepOrder,
        String approvalGroupCode,
        String description
) {
    public static ApprovalTemplateStepResponse from(ApprovalTemplateStep step) {
        return new ApprovalTemplateStepResponse(
                step.getId(),
                step.getStepOrder(),
                step.getApprovalGroupCode(),
                step.getDescription()
        );
    }
}
