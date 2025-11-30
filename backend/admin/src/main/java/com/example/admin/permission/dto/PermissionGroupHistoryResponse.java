package com.example.admin.permission.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import com.example.admin.permission.domain.PermissionGroup;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;

/**
 * 권한 그룹 버전 이력 응답 DTO (SCD Type 2).
 */
public record PermissionGroupHistoryResponse(
        UUID id,
        UUID groupId,
        String groupCode,
        Integer version,
        OffsetDateTime validFrom,
        OffsetDateTime validTo,
        String name,
        String description,
        boolean active,
        List<PermissionAssignmentDto> assignments,
        List<String> approvalGroupCodes,
        VersionStatus status,
        ChangeAction changeAction,
        String changeReason,
        String changedBy,
        String changedByName,
        OffsetDateTime changedAt,
        Integer rollbackFromVersion,
        String versionTag
) {
    public static PermissionGroupHistoryResponse from(PermissionGroup version) {
        return from(version, UnaryOperator.identity());
    }

    public static PermissionGroupHistoryResponse from(PermissionGroup version, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;

        List<PermissionAssignmentDto> assignmentDtos = version.getAssignments().stream()
                .map(PermissionAssignmentDto::from)
                .collect(Collectors.toList());

        return new PermissionGroupHistoryResponse(
                version.getId(),
                version.getRoot().getId(),
                version.getRoot().getGroupCode(),
                version.getVersion(),
                version.getValidFrom(),
                version.getValidTo(),
                fn.apply(version.getName()),
                fn.apply(version.getDescription()),
                version.isActive(),
                assignmentDtos,
                version.getApprovalGroupCodes(),
                version.getStatus(),
                version.getChangeAction(),
                fn.apply(version.getChangeReason()),
                fn.apply(version.getChangedBy()),
                fn.apply(version.getChangedByName()),
                version.getChangedAt(),
                version.getRollbackFromVersion(),
                version.getVersionTag()
        );
    }
}
