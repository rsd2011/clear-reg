package com.example.policy.dto;

import java.util.List;

public record PolicyView(boolean passwordPolicyEnabled,
                         boolean passwordHistoryEnabled,
                         boolean accountLockEnabled,
                         List<String> enabledLoginTypes,
                         long maxFileSizeBytes,
                         List<String> allowedFileExtensions,
                         boolean strictMimeValidation,
                         int fileRetentionDays,
                         boolean auditEnabled,
                         boolean auditReasonRequired,
                         boolean auditSensitiveApiDefaultOn,
                         int auditRetentionDays,
                         boolean auditStrictMode,
                         String auditRiskLevel,
                         boolean auditMaskingEnabled,
                         List<String> auditSensitiveEndpoints,
                         List<String> auditUnmaskRoles,
                         boolean auditPartitionEnabled,
                         String auditPartitionCron,
                         int auditPartitionPreloadMonths,
                         String yaml) {
}
