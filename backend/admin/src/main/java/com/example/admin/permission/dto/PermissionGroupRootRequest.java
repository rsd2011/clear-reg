package com.example.admin.permission.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 권한 그룹 생성/수정 요청 DTO.
 */
public record PermissionGroupRootRequest(
        @Size(max = 100, message = "그룹 코드는 100자 이하여야 합니다")
        String groupCode,

        @NotBlank(message = "그룹명은 필수입니다")
        @Size(max = 255, message = "그룹명은 255자 이하여야 합니다")
        String name,

        @Size(max = 1000, message = "설명은 1000자 이하여야 합니다")
        String description,

        boolean active,

        @Valid
        List<PermissionAssignmentDto> assignments,

        List<String> approvalGroupCodes
) {
    public PermissionGroupRootRequest {
        if (assignments == null) {
            assignments = List.of();
        }
        if (approvalGroupCodes == null) {
            approvalGroupCodes = List.of();
        }
    }
}
