package com.example.admin.rowaccesspolicy.dto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.UnaryOperator;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.admin.rowaccesspolicy.domain.RowAccessPolicy;
import com.example.common.security.RowScope;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;

/**
 * 행 접근 정책 버전 이력 응답 DTO (SCD Type 2).
 */
public record RowAccessPolicyHistoryResponse(
        UUID id,
        UUID policyId,
        Integer version,
        OffsetDateTime validFrom,
        OffsetDateTime validTo,
        String name,
        String description,
        FeatureCode featureCode,
        ActionCode actionCode,
        String permGroupCode,
        String orgGroupCode,
        RowScope rowScope,
        Integer priority,
        boolean active,
        Instant effectiveFrom,
        Instant effectiveTo,
        VersionStatus status,
        ChangeAction changeAction,
        String changeReason,
        String changedBy,
        String changedByName,
        OffsetDateTime changedAt,
        Integer rollbackFromVersion,
        String versionTag
) {
    public static RowAccessPolicyHistoryResponse from(RowAccessPolicy version) {
        return from(version, UnaryOperator.identity());
    }

    public static RowAccessPolicyHistoryResponse from(RowAccessPolicy version, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;

        return new RowAccessPolicyHistoryResponse(
                version.getId(),
                version.getRoot().getId(),
                version.getVersion(),
                version.getValidFrom(),
                version.getValidTo(),
                fn.apply(version.getName()),
                fn.apply(version.getDescription()),
                version.getFeatureCode(),
                version.getActionCode(),
                fn.apply(version.getPermGroupCode()),
                fn.apply(version.getOrgGroupCode()),
                version.getRowScope(),
                version.getPriority(),
                version.isActive(),
                version.getEffectiveFrom(),
                version.getEffectiveTo(),
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
