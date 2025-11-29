package com.example.admin.approval.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApprovalTemplateStepRequest(
        @Min(1) int stepOrder,
        @NotBlank @Size(max = 64) String approvalGroupCode,
        boolean skippable
) {
    /**
     * 하위 호환성을 위한 생성자 (skippable 기본값 false).
     */
    public ApprovalTemplateStepRequest(int stepOrder, String approvalGroupCode) {
        this(stepOrder, approvalGroupCode, false);
    }
}
