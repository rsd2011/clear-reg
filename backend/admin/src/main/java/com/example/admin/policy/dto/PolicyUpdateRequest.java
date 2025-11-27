package com.example.admin.policy.dto;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import com.example.common.schedule.BatchJobCode;
import com.example.common.schedule.BatchJobSchedule;

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
        @PositiveOrZero Integer auditPartitionPreloadMonths,
        String auditPartitionTablespaceHot,
        String auditPartitionTablespaceCold,
        @PositiveOrZero Integer auditPartitionHotMonths,
        @PositiveOrZero Integer auditPartitionColdMonths,
        Boolean auditMonthlyReportEnabled,
        String auditMonthlyReportCron,
        Boolean auditLogRetentionEnabled,
        String auditLogRetentionCron,
        Boolean auditColdArchiveEnabled,
        String auditColdArchiveCron,
        Boolean auditRetentionCleanupEnabled,
        String auditRetentionCleanupCron,
        Map<BatchJobCode, BatchJobSchedule> batchJobs) {

    // 이전 시그니처 호환용 보조 생성자
    public PolicyUpdateRequest(Boolean passwordPolicyEnabled,
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
                               @PositiveOrZero Integer auditPartitionPreloadMonths,
                               String auditPartitionTablespaceHot,
                               String auditPartitionTablespaceCold,
                               @PositiveOrZero Integer auditPartitionHotMonths,
                               @PositiveOrZero Integer auditPartitionColdMonths,
                               Boolean auditMonthlyReportEnabled,
                               String auditMonthlyReportCron,
                               Boolean auditLogRetentionEnabled,
                               String auditLogRetentionCron,
                               Boolean auditColdArchiveEnabled,
                               String auditColdArchiveCron,
                               Boolean auditRetentionCleanupEnabled,
                               String auditRetentionCleanupCron) {
        this(passwordPolicyEnabled, passwordHistoryEnabled, accountLockEnabled, enabledLoginTypes, maxFileSizeBytes,
                allowedFileExtensions, strictMimeValidation, fileRetentionDays,
                auditEnabled, auditReasonRequired, auditSensitiveApiDefaultOn, auditRetentionDays,
                auditStrictMode, auditRiskLevel, auditMaskingEnabled, auditSensitiveEndpoints, auditUnmaskRoles,
                auditPartitionEnabled, auditPartitionCron, auditPartitionPreloadMonths,
                auditPartitionTablespaceHot, auditPartitionTablespaceCold, auditPartitionHotMonths, auditPartitionColdMonths,
                auditMonthlyReportEnabled, auditMonthlyReportCron,
                auditLogRetentionEnabled, auditLogRetentionCron,
                auditColdArchiveEnabled, auditColdArchiveCron,
                auditRetentionCleanupEnabled, auditRetentionCleanupCron,
                Map.of());
    }
}
