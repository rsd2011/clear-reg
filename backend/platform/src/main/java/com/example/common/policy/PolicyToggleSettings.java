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
                                   List<String> auditSensitiveEndpoints) {

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
                true, true, true, 730, true, "MEDIUM", List.of());
    }
}
