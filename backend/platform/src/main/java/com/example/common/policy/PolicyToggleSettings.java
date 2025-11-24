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
                                   @JsonProperty(defaultValue = "1") int auditPartitionPreloadMonths,
                                   @JsonProperty(defaultValue = "true") boolean auditMonthlyReportEnabled,
                                   @JsonProperty(defaultValue = "0 0 4 1 * *") String auditMonthlyReportCron,
                                   @JsonProperty(defaultValue = "true") boolean auditLogRetentionEnabled,
                                   @JsonProperty(defaultValue = "0 0 3 * * *") String auditLogRetentionCron,
                                   @JsonProperty(defaultValue = "false") boolean auditColdArchiveEnabled,
                                   @JsonProperty(defaultValue = "0 30 2 2 * *") String auditColdArchiveCron,
                                   @JsonProperty(defaultValue = "true") boolean auditRetentionCleanupEnabled,
                                   @JsonProperty(defaultValue = "0 30 3 * * *") String auditRetentionCleanupCron,
                                   @JsonProperty(defaultValue = "{}") java.util.Map<com.example.common.schedule.BatchJobCode, com.example.common.schedule.BatchJobSchedule> batchJobs) {

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
        if (auditMonthlyReportCron == null || auditMonthlyReportCron.isBlank()) {
            auditMonthlyReportCron = "0 0 4 1 * *";
        }
        if (auditLogRetentionCron == null || auditLogRetentionCron.isBlank()) {
            auditLogRetentionCron = "0 0 3 * * *";
        }
        if (auditColdArchiveCron == null || auditColdArchiveCron.isBlank()) {
            auditColdArchiveCron = "0 30 2 2 * *";
        }
        if (auditRetentionCleanupCron == null || auditRetentionCleanupCron.isBlank()) {
            auditRetentionCleanupCron = "0 30 3 * * *";
        }
        batchJobs = batchJobs == null ? com.example.common.schedule.BatchJobDefaults.defaults() : java.util.Map.copyOf(batchJobs);
    }

    /** 새 필드(batchJobs) 이전 시그니처 호환용 전체 인자 생성자. */
    public PolicyToggleSettings(boolean passwordPolicyEnabled,
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
                                @JsonProperty(defaultValue = "1") int auditPartitionPreloadMonths,
                                @JsonProperty(defaultValue = "true") boolean auditMonthlyReportEnabled,
                                @JsonProperty(defaultValue = "0 0 4 1 * *") String auditMonthlyReportCron,
                                @JsonProperty(defaultValue = "true") boolean auditLogRetentionEnabled,
                                @JsonProperty(defaultValue = "0 0 3 * * *") String auditLogRetentionCron,
                                @JsonProperty(defaultValue = "false") boolean auditColdArchiveEnabled,
                                @JsonProperty(defaultValue = "0 30 2 2 * *") String auditColdArchiveCron,
                                @JsonProperty(defaultValue = "true") boolean auditRetentionCleanupEnabled,
                                @JsonProperty(defaultValue = "0 30 3 * * *") String auditRetentionCleanupCron) {
        this(passwordPolicyEnabled, passwordHistoryEnabled, accountLockEnabled, enabledLoginTypes, maxFileSizeBytes,
                allowedFileExtensions, strictMimeValidation, fileRetentionDays,
                auditEnabled, auditReasonRequired, auditSensitiveApiDefaultOn, auditRetentionDays,
                auditStrictMode, auditRiskLevel, auditMaskingEnabled, auditSensitiveEndpoints, auditUnmaskRoles,
                auditPartitionEnabled, auditPartitionCron, auditPartitionPreloadMonths,
                auditMonthlyReportEnabled, auditMonthlyReportCron,
                auditLogRetentionEnabled, auditLogRetentionCron,
                auditColdArchiveEnabled, auditColdArchiveCron,
                auditRetentionCleanupEnabled, auditRetentionCleanupCron,
                com.example.common.schedule.BatchJobDefaults.defaults());
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
                        false, "0 0 2 1 * *", 1,
                        true, "0 0 4 1 * *",
                        true, "0 0 3 * * *",
                        false, "0 30 2 2 * *",
                        true, "0 30 3 * * *",
                        com.example.common.schedule.BatchJobDefaults.defaults());
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
                        false, "0 0 2 1 * *", 1,
                        true, "0 0 4 1 * *",
                        true, "0 0 3 * * *",
                        false, "0 30 2 2 * *",
                        true, "0 30 3 * * *",
                        com.example.common.schedule.BatchJobDefaults.defaults());
    }

    public List<String> auditUnmaskRoles() {
        return auditUnmaskRoles;
    }
}
