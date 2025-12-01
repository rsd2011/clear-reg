package com.example.batch.audit;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;
import com.example.common.schedule.BatchJobDefaults;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

@TestConfiguration
public class TestObjectMapperConfig {

    @Bean
    @Primary
    public ObjectMapper primaryObjectMapper(Jackson2ObjectMapperBuilder builder) {
        return builder.build();
    }

    /**
     * 테스트용 PolicySettingsProvider.
     * SystemConfigPolicySettingsProvider가 활성화되지 않는 테스트 환경에서 사용됩니다.
     */
    @Bean
    @Primary
    public PolicySettingsProvider testPolicySettingsProvider() {
        return () -> new PolicyToggleSettings(
                true,  // passwordPolicyEnabled
                true,  // passwordHistoryEnabled
                true,  // accountLockEnabled
                List.of("ID_PASSWORD"),  // enabledLoginTypes
                10485760L,  // maxFileSizeBytes (10MB)
                List.of("pdf", "doc", "docx", "xls", "xlsx"),  // allowedFileExtensions
                true,  // strictMimeValidation
                365,   // fileRetentionDays
                true,  // auditEnabled
                false, // auditReasonRequired
                false, // auditSensitiveApiDefaultOn
                365,   // auditRetentionDays
                false, // auditStrictMode
                "MEDIUM", // auditRiskLevel
                true,  // auditMaskingEnabled
                List.of(),  // auditSensitiveEndpoints
                List.of("ADMIN"),  // auditUnmaskRoles
                true,  // auditPartitionEnabled
                "0 0 1 * * ?",  // auditPartitionCron
                3,     // auditPartitionPreloadMonths
                true,  // auditMonthlyReportEnabled
                "0 0 2 1 * ?",  // auditMonthlyReportCron
                true,  // auditLogRetentionEnabled
                "0 0 3 * * ?",  // auditLogRetentionCron
                false, // auditColdArchiveEnabled
                "0 0 4 * * ?",  // auditColdArchiveCron
                true,  // auditRetentionCleanupEnabled
                "0 0 5 * * ?",  // auditRetentionCleanupCron
                BatchJobDefaults.defaults()  // batchJobs
        );
    }
}
