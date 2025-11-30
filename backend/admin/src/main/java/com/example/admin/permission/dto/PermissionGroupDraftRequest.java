package com.example.admin.permission.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 권한 그룹 초안 저장 요청 DTO.
 */
public record PermissionGroupDraftRequest(
        @NotBlank(message = "그룹명은 필수입니다")
        @Size(max = 255, message = "그룹명은 255자 이하여야 합니다")
        String name,

        @Size(max = 1000, message = "설명은 1000자 이하여야 합니다")
        String description,

        boolean active,

        @Valid
        List<PermissionAssignmentDto> assignments,

        List<String> approvalGroupCodes,

        @Size(max = 500, message = "변경 사유는 500자 이하여야 합니다")
        String changeReason
) {
    public PermissionGroupDraftRequest {
        if (assignments == null) {
            assignments = List.of();
        }
        if (approvalGroupCodes == null) {
            approvalGroupCodes = List.of();
        }
    }
}
