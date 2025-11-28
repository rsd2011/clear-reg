package com.example.admin.approval.dto;

import java.util.UUID;

import com.example.admin.approval.domain.ApprovalTemplateStep;

public record ApprovalTemplateStepResponse(
        UUID id,
        int stepOrder,
        String approvalGroupCode,
        String approvalGroupName
) {
    public static ApprovalTemplateStepResponse from(ApprovalTemplateStep step) {
        return new ApprovalTemplateStepResponse(
                step.getId(),
                step.getStepOrder(),
                step.getApprovalGroup().getGroupCode(),
                step.getApprovalGroup().getName()
        );
    }
}
