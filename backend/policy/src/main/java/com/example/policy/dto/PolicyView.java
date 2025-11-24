package com.example.policy.dto;

import java.util.List;
import java.util.Map;

import com.example.common.schedule.BatchJobCode;
import com.example.common.schedule.BatchJobSchedule;

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
                         String auditPartitionTablespaceHot,
                         String auditPartitionTablespaceCold,
                         int auditPartitionHotMonths,
                         int auditPartitionColdMonths,
                         boolean auditMonthlyReportEnabled,
                         String auditMonthlyReportCron,
                         boolean auditLogRetentionEnabled,
                         String auditLogRetentionCron,
                         boolean auditColdArchiveEnabled,
                         String auditColdArchiveCron,
                         boolean auditRetentionCleanupEnabled,
                         String auditRetentionCleanupCron,
                         Map<BatchJobCode, BatchJobSchedule> batchJobs,
                         String yaml) {
}
