package com.example.admin.approval.dto;

import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * 버전 비교 응답 DTO.
 * 두 버전 간의 차이점을 필드별로 제공합니다.
 */
public record VersionComparisonResponse(
        UUID templateId,
        String templateCode,
        VersionSummary version1,
        VersionSummary version2,
        List<FieldDiff> fieldDiffs,
        List<StepDiff> stepDiffs
) {
    /**
     * 버전 요약 정보.
     */
    public record VersionSummary(
            Integer version,
            String changedBy,
            String changedByName,
            String changedAt,
            String changeAction,
            String changeReason
    ) {}

    /**
     * 필드 변경 정보.
     */
    public record FieldDiff(
            String fieldName,
            String fieldLabel,
            Object beforeValue,
            Object afterValue,
            DiffType diffType
    ) {}

    /**
     * Step 변경 정보.
     */
    public record StepDiff(
            int stepOrder,
            String approvalGroupCode,
            String beforeGroupName,
            String afterGroupName,
            DiffType diffType
    ) {}

    /**
     * 변경 유형.
     */
    public enum DiffType {
        ADDED,      // 추가됨
        REMOVED,    // 삭제됨
        MODIFIED,   // 수정됨
        UNCHANGED   // 변경 없음
    }

    public static VersionComparisonResponse apply(VersionComparisonResponse response, UnaryOperator<String> masker) {
        if (masker == null) {
            return response;
        }

        VersionSummary maskedVersion1 = new VersionSummary(
                response.version1().version(),
                masker.apply(response.version1().changedBy()),
                masker.apply(response.version1().changedByName()),
                response.version1().changedAt(),
                response.version1().changeAction(),
                masker.apply(response.version1().changeReason())
        );

        VersionSummary maskedVersion2 = new VersionSummary(
                response.version2().version(),
                masker.apply(response.version2().changedBy()),
                masker.apply(response.version2().changedByName()),
                response.version2().changedAt(),
                response.version2().changeAction(),
                masker.apply(response.version2().changeReason())
        );

        List<FieldDiff> maskedFieldDiffs = response.fieldDiffs().stream()
                .map(fd -> {
                    Object maskedBefore = fd.beforeValue() instanceof String s ? masker.apply(s) : fd.beforeValue();
                    Object maskedAfter = fd.afterValue() instanceof String s ? masker.apply(s) : fd.afterValue();
                    return new FieldDiff(fd.fieldName(), fd.fieldLabel(), maskedBefore, maskedAfter, fd.diffType());
                })
                .toList();

        return new VersionComparisonResponse(
                response.templateId(),
                masker.apply(response.templateCode()),
                maskedVersion1,
                maskedVersion2,
                maskedFieldDiffs,
                response.stepDiffs()
        );
    }
}
