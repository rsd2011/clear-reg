package com.example.admin.maskingpolicy.dto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import com.example.common.masking.DataKind;
import com.example.admin.maskingpolicy.domain.MaskingPolicy;
import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;

/**
 * 마스킹 정책 버전 이력 응답 DTO (SCD Type 2).
 */
public record MaskingPolicyHistoryResponse(
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
        Set<String> dataKinds,
        Boolean maskingEnabled,
        Boolean auditEnabled,
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
    public static MaskingPolicyHistoryResponse from(MaskingPolicy version) {
        return from(version, UnaryOperator.identity());
    }

    public static MaskingPolicyHistoryResponse from(MaskingPolicy version, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;

        Set<String> dataKindStrings = version.getDataKinds() != null
                ? version.getDataKinds().stream()
                        .map(DataKind::name)
                        .collect(Collectors.toSet())
                : Set.of();

        return new MaskingPolicyHistoryResponse(
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
                dataKindStrings,
                version.getMaskingEnabled(),
                version.getAuditEnabled(),
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
