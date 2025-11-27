package com.example.admin.approval.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApprovalTemplateStepRequest(
        @Min(1) int stepOrder,
        @NotBlank @Size(max = 64) String approvalGroupCode
) {
}
