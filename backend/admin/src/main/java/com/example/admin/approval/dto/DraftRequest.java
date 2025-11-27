package com.example.admin.approval.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 초안 생성/수정 요청 DTO.
 */
public record DraftRequest(
        @NotBlank(message = "이름은 필수입니다")
        @Size(max = 255, message = "이름은 255자 이하여야 합니다")
        String name,

        Integer displayOrder,

        @Size(max = 500, message = "설명은 500자 이하여야 합니다")
        String description,

        boolean active,

        @Size(max = 500, message = "변경 사유는 500자 이하여야 합니다")
        String changeReason,

        @Valid
        List<ApprovalTemplateStepRequest> steps
) {
    public DraftRequest {
        if (steps == null) {
            steps = List.of();
        }
    }
}
