package com.example.admin.permission.dto;

import com.example.admin.permission.domain.PermissionAssignment;
import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import jakarta.validation.constraints.NotNull;

/**
 * 권한 할당 DTO.
 */
public record PermissionAssignmentDto(
        @NotNull(message = "기능 코드는 필수입니다")
        FeatureCode feature,
        @NotNull(message = "액션 코드는 필수입니다")
        ActionCode action
) {
    public static PermissionAssignmentDto from(PermissionAssignment assignment) {
        return new PermissionAssignmentDto(
                assignment.getFeature(),
                assignment.getAction()
        );
    }

    public PermissionAssignment toEntity() {
        return new PermissionAssignment(feature, action);
    }
}
