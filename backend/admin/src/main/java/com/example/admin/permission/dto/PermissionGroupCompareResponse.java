package com.example.admin.permission.dto;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.admin.permission.domain.PermissionAssignment;
import com.example.admin.permission.domain.PermissionGroup;

/**
 * 권한 그룹 버전 비교 응답 DTO.
 */
public record PermissionGroupCompareResponse(
        PermissionGroupHistoryResponse version1,
        PermissionGroupHistoryResponse version2,
        List<PermissionAssignmentDto> addedAssignments,
        List<PermissionAssignmentDto> removedAssignments,
        List<String> addedApprovalGroups,
        List<String> removedApprovalGroups,
        boolean nameChanged,
        boolean descriptionChanged,
        boolean activeChanged
) {
    public static PermissionGroupCompareResponse from(PermissionGroup v1, PermissionGroup v2) {
        Set<PermissionAssignment> v1Assignments = Set.copyOf(v1.getAssignments());
        Set<PermissionAssignment> v2Assignments = Set.copyOf(v2.getAssignments());

        // v2에는 있지만 v1에는 없는 것 = 추가됨
        List<PermissionAssignmentDto> added = v2Assignments.stream()
                .filter(a -> !v1Assignments.contains(a))
                .map(PermissionAssignmentDto::from)
                .collect(Collectors.toList());

        // v1에는 있지만 v2에는 없는 것 = 삭제됨
        List<PermissionAssignmentDto> removed = v1Assignments.stream()
                .filter(a -> !v2Assignments.contains(a))
                .map(PermissionAssignmentDto::from)
                .collect(Collectors.toList());

        Set<String> v1ApprovalGroups = Set.copyOf(v1.getApprovalGroupCodes());
        Set<String> v2ApprovalGroups = Set.copyOf(v2.getApprovalGroupCodes());

        List<String> addedApprovalGroups = v2ApprovalGroups.stream()
                .filter(g -> !v1ApprovalGroups.contains(g))
                .collect(Collectors.toList());

        List<String> removedApprovalGroups = v1ApprovalGroups.stream()
                .filter(g -> !v2ApprovalGroups.contains(g))
                .collect(Collectors.toList());

        boolean nameChanged = !nullSafeEquals(v1.getName(), v2.getName());
        boolean descriptionChanged = !nullSafeEquals(v1.getDescription(), v2.getDescription());
        boolean activeChanged = v1.isActive() != v2.isActive();

        return new PermissionGroupCompareResponse(
                PermissionGroupHistoryResponse.from(v1),
                PermissionGroupHistoryResponse.from(v2),
                added,
                removed,
                addedApprovalGroups,
                removedApprovalGroups,
                nameChanged,
                descriptionChanged,
                activeChanged
        );
    }

    private static boolean nullSafeEquals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
