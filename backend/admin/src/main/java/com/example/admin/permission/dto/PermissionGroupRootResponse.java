package com.example.admin.permission.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import com.example.admin.permission.domain.PermissionGroup;
import com.example.admin.permission.domain.PermissionGroupRoot;

/**
 * 권한 그룹 루트 응답 DTO.
 */
public record PermissionGroupRootResponse(
        UUID id,
        String groupCode,
        String name,
        String description,
        boolean active,
        List<PermissionAssignmentDto> assignments,
        List<String> approvalGroupCodes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Integer currentVersion,
        boolean hasDraft
) {
    public static PermissionGroupRootResponse from(PermissionGroupRoot root) {
        return from(root, UnaryOperator.identity());
    }

    public static PermissionGroupRootResponse from(PermissionGroupRoot root, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;

        PermissionGroup currentVersion = root.getCurrentVersion();

        List<PermissionAssignmentDto> assignmentDtos = currentVersion != null
                ? currentVersion.getAssignments().stream()
                        .map(PermissionAssignmentDto::from)
                        .collect(Collectors.toList())
                : List.of();

        List<String> approvalGroupCodes = currentVersion != null
                ? currentVersion.getApprovalGroupCodes()
                : List.of();

        return new PermissionGroupRootResponse(
                root.getId(),
                fn.apply(root.getGroupCode()),
                fn.apply(root.getName()),
                fn.apply(root.getDescription()),
                root.isActive(),
                assignmentDtos,
                approvalGroupCodes,
                root.getCreatedAt(),
                root.getUpdatedAt(),
                root.getCurrentVersionNumber(),
                root.hasDraft()
        );
    }
}
