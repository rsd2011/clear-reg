package com.example.admin.maskingpolicy.dto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import com.example.common.masking.DataKind;
import com.example.admin.maskingpolicy.domain.MaskingPolicyRoot;
import com.example.admin.maskingpolicy.domain.MaskingPolicyVersion;
import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;

/**
 * 마스킹 정책 루트 응답 DTO.
 */
public record MaskingPolicyRootResponse(
        UUID id,
        String policyCode,
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
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Integer currentVersion,
        boolean hasDraft
) {
    public static MaskingPolicyRootResponse from(MaskingPolicyRoot root) {
        return from(root, UnaryOperator.identity());
    }

    public static MaskingPolicyRootResponse from(MaskingPolicyRoot root, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;

        MaskingPolicyVersion currentVersion = root.getCurrentVersion();

        Set<String> dataKindStrings = currentVersion != null && currentVersion.getDataKinds() != null
                ? currentVersion.getDataKinds().stream()
                        .map(DataKind::name)
                        .collect(Collectors.toSet())
                : Set.of();

        return new MaskingPolicyRootResponse(
                root.getId(),
                fn.apply(root.getPolicyCode()),
                fn.apply(root.getName()),
                fn.apply(root.getDescription()),
                currentVersion != null ? currentVersion.getFeatureCode() : null,
                currentVersion != null ? currentVersion.getActionCode() : null,
                currentVersion != null ? fn.apply(currentVersion.getPermGroupCode()) : null,
                currentVersion != null ? fn.apply(currentVersion.getOrgGroupCode()) : null,
                dataKindStrings,
                currentVersion != null ? currentVersion.getMaskingEnabled() : null,
                currentVersion != null ? currentVersion.getAuditEnabled() : null,
                root.getPriority(),
                root.isActive(),
                currentVersion != null ? currentVersion.getEffectiveFrom() : null,
                currentVersion != null ? currentVersion.getEffectiveTo() : null,
                root.getCreatedAt(),
                root.getUpdatedAt(),
                root.getCurrentVersionNumber(),
                root.hasDraft()
        );
    }
}
