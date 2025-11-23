package com.example.policy.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record PolicyUpdateRequest(
        Boolean passwordPolicyEnabled,
        Boolean passwordHistoryEnabled,
        Boolean accountLockEnabled,
        List<@NotNull String> enabledLoginTypes,
        @Positive Long maxFileSizeBytes,
        List<@NotBlank String> allowedFileExtensions,
        Boolean strictMimeValidation,
        @PositiveOrZero Integer fileRetentionDays,
        Boolean auditEnabled,
        Boolean auditReasonRequired,
        Boolean auditSensitiveApiDefaultOn,
        @PositiveOrZero Integer auditRetentionDays,
        Boolean auditStrictMode,
        String auditRiskLevel,
        Boolean auditMaskingEnabled,
        List<String> auditSensitiveEndpoints,
        List<String> auditUnmaskRoles,
        Boolean auditPartitionEnabled,
        String auditPartitionCron,
        @PositiveOrZero Integer auditPartitionPreloadMonths) {

}
