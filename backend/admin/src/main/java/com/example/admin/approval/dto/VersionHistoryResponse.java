package com.example.admin.approval.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

import com.example.admin.approval.domain.ApprovalTemplate;
import com.example.admin.approval.domain.ApprovalTemplateStep;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;

/**
 * 버전 이력 응답 DTO (SCD Type 2).
 */
public record VersionHistoryResponse(
        UUID id,
        UUID templateId,
        Integer version,
        OffsetDateTime validFrom,
        OffsetDateTime validTo,
        String name,
        Integer displayOrder,
        String description,
        boolean active,
        VersionStatus status,
        ChangeAction changeAction,
        String changeReason,
        String changedBy,
        String changedByName,
        OffsetDateTime changedAt,
        Integer rollbackFromVersion,
        String versionTag,
        List<VersionStepResponse> steps
) {
    public static VersionHistoryResponse from(ApprovalTemplate version) {
        return from(version, UnaryOperator.identity());
    }

    public static VersionHistoryResponse from(ApprovalTemplate version, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;

        List<VersionStepResponse> stepResponses = version.getSteps().stream()
                .map(VersionStepResponse::from)
                .toList();

        return new VersionHistoryResponse(
                version.getId(),
                version.getRoot().getId(),
                version.getVersion(),
                version.getValidFrom(),
                version.getValidTo(),
                fn.apply(version.getName()),
                version.getDisplayOrder(),
                fn.apply(version.getDescription()),
                version.isActive(),
                version.getStatus(),
                version.getChangeAction(),
                fn.apply(version.getChangeReason()),
                fn.apply(version.getChangedBy()),
                fn.apply(version.getChangedByName()),
                version.getChangedAt(),
                version.getRollbackFromVersion(),
                version.getVersionTag(),
                stepResponses
        );
    }

    public static VersionHistoryResponse apply(VersionHistoryResponse response, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        return new VersionHistoryResponse(
                response.id(),
                response.templateId(),
                response.version(),
                response.validFrom(),
                response.validTo(),
                fn.apply(response.name()),
                response.displayOrder(),
                fn.apply(response.description()),
                response.active(),
                response.status(),
                response.changeAction(),
                fn.apply(response.changeReason()),
                fn.apply(response.changedBy()),
                fn.apply(response.changedByName()),
                response.changedAt(),
                response.rollbackFromVersion(),
                response.versionTag(),
                response.steps()
        );
    }

    /**
     * 버전의 Step 응답 DTO.
     */
    public record VersionStepResponse(
            UUID id,
            int stepOrder,
            UUID approvalGroupId,
            String approvalGroupCode,
            String approvalGroupName
    ) {
        public static VersionStepResponse from(ApprovalTemplateStep step) {
            return new VersionStepResponse(
                    step.getId(),
                    step.getStepOrder(),
                    step.getApprovalGroupId(),
                    step.getApprovalGroupCode(),
                    step.getApprovalGroupName()
            );
        }
    }
}
