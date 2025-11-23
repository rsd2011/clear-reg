package com.example.common.policy;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Immutable DTO describing organization level policy toggles that other modules consume.
 */
public record PolicyToggleSettings(boolean passwordPolicyEnabled,
                                   boolean passwordHistoryEnabled,
                                   boolean accountLockEnabled,
                                   List<String> enabledLoginTypes,
                                   @JsonProperty(defaultValue = "20971520") long maxFileSizeBytes,
                                   List<String> allowedFileExtensions,
                                   @JsonProperty(defaultValue = "true") boolean strictMimeValidation,
                                   @JsonProperty(defaultValue = "365") int fileRetentionDays,
                                   @JsonProperty(defaultValue = "true") boolean auditEnabled,
                                   @JsonProperty(defaultValue = "true") boolean auditReasonRequired,
                                   @JsonProperty(defaultValue = "true") boolean auditSensitiveApiDefaultOn,
                                   @JsonProperty(defaultValue = "730") int auditRetentionDays,
                                   @JsonProperty(defaultValue = "true") boolean auditStrictMode,
                                   @JsonProperty(defaultValue = "MEDIUM") String auditRiskLevel,
                                   @JsonProperty(defaultValue = "true") boolean auditMaskingEnabled,
                                   List<String> auditSensitiveEndpoints,
                                   List<String> auditUnmaskRoles,
                                   @JsonProperty(defaultValue = "false") boolean auditPartitionEnabled,
                                   @JsonProperty(defaultValue = "0 0 2 1 * *") String auditPartitionCron,
                                   @JsonProperty(defaultValue = "1") int auditPartitionPreloadMonths) {

    private static final long DEFAULT_MAX_FILE_SIZE = 20 * 1024 * 1024;

    public PolicyToggleSettings {
        enabledLoginTypes = enabledLoginTypes == null ? List.of() : List.copyOf(enabledLoginTypes);
        allowedFileExtensions = allowedFileExtensions == null ? List.of() : allowedFileExtensions.stream()
                .map(ext -> ext.toLowerCase())
                .distinct()
                .toList();
        if (maxFileSizeBytes <= 0) {
            maxFileSizeBytes = DEFAULT_MAX_FILE_SIZE;
        }
        if (fileRetentionDays < 0) {
            fileRetentionDays = 0;
        }
        if (auditRetentionDays < 0) {
            auditRetentionDays = 0;
        }
        auditRiskLevel = auditRiskLevel == null ? "MEDIUM" : auditRiskLevel.toUpperCase();
        auditSensitiveEndpoints = auditSensitiveEndpoints == null ? List.of() : List.copyOf(auditSensitiveEndpoints);
        auditUnmaskRoles = auditUnmaskRoles == null ? List.of() : List.copyOf(auditUnmaskRoles);
        if (auditPartitionCron == null || auditPartitionCron.isBlank()) {
            auditPartitionCron = "0 0 2 1 * *";
        }
        if (auditPartitionPreloadMonths < 0) {
            auditPartitionPreloadMonths = 0;
        }
    }

    /** 기존 시그니처 호환을 위한 편의 생성자. */
    public PolicyToggleSettings(boolean passwordPolicyEnabled,
                                boolean passwordHistoryEnabled,
                                boolean accountLockEnabled,
                                List<String> enabledLoginTypes,
                                long maxFileSizeBytes,
                                List<String> allowedFileExtensions,
                                boolean strictMimeValidation,
                                int fileRetentionDays) {
        this(passwordPolicyEnabled, passwordHistoryEnabled, accountLockEnabled, enabledLoginTypes, maxFileSizeBytes,
                allowedFileExtensions, strictMimeValidation, fileRetentionDays,
                true, true, true, 730, true, "MEDIUM", true, List.of(), List.of(),
                false, "0 0 2 1 * *", 1);
    }

    /** 이전 시그니처(추가 파티션 필드 이전) 호환용 생성자. */
    public PolicyToggleSettings(boolean passwordPolicyEnabled,
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
                                List<String> auditUnmaskRoles) {
        this(passwordPolicyEnabled, passwordHistoryEnabled, accountLockEnabled, enabledLoginTypes, maxFileSizeBytes,
                allowedFileExtensions, strictMimeValidation, fileRetentionDays,
                auditEnabled, auditReasonRequired, auditSensitiveApiDefaultOn, auditRetentionDays,
                auditStrictMode, auditRiskLevel, auditMaskingEnabled, auditSensitiveEndpoints, auditUnmaskRoles,
                false, "0 0 2 1 * *", 1);
    }

    public List<String> auditUnmaskRoles() {
        return auditUnmaskRoles;
    }
}
