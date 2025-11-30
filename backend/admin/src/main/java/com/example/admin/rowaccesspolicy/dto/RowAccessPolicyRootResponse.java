package com.example.admin.rowaccesspolicy.dto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.UnaryOperator;

import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.admin.rowaccesspolicy.domain.RowAccessPolicy;
import com.example.admin.rowaccesspolicy.domain.RowAccessPolicyRoot;
import com.example.common.security.RowScope;

/**
 * 행 접근 정책 루트 응답 DTO.
 */
public record RowAccessPolicyRootResponse(
        UUID id,
        String policyCode,
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
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Integer currentVersion,
        boolean hasDraft
) {
    public static RowAccessPolicyRootResponse from(RowAccessPolicyRoot root) {
        return from(root, UnaryOperator.identity());
    }

    public static RowAccessPolicyRootResponse from(RowAccessPolicyRoot root, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;

        RowAccessPolicy currentVersion = root.getCurrentVersion();

        return new RowAccessPolicyRootResponse(
                root.getId(),
                fn.apply(root.getPolicyCode()),
                fn.apply(root.getName()),
                fn.apply(root.getDescription()),
                currentVersion != null ? currentVersion.getFeatureCode() : null,
                currentVersion != null ? currentVersion.getActionCode() : null,
                currentVersion != null ? fn.apply(currentVersion.getPermGroupCode()) : null,
                currentVersion != null ? fn.apply(currentVersion.getOrgGroupCode()) : null,
                root.getRowScope(),
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
