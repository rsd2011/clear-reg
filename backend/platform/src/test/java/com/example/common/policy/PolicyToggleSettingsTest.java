package com.example.common.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class PolicyToggleSettingsTest {

    @Test
    void whenLoginTypesAbsent_thenDefaultsToEmptyList() {
        PolicyToggleSettings settings = new PolicyToggleSettings(true, true, false, null, 1024L, null, true, 30);

        assertThat(settings.enabledLoginTypes()).isEmpty();
    }

    @Test
    void whenLoginTypesProvided_thenDefensiveCopyIsImmutable() {
        List<String> original = new ArrayList<>(List.of("SSO", "PASSWORD"));

        PolicyToggleSettings settings = new PolicyToggleSettings(true, true, false, original, 1024L, List.of("PDF"), true, 15);
        original.add("AD");

        assertThat(settings.enabledLoginTypes()).containsExactly("SSO", "PASSWORD");
        assertThatThrownBy(() -> settings.enabledLoginTypes().add("NEW"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(settings.allowedFileExtensions()).containsExactly("pdf");
    }

    @Test
    void whenFileSettingsInvalid_thenDefaultsAndNormalizationApplied() {
        PolicyToggleSettings settings = new PolicyToggleSettings(true, true, false,
                List.of("PASSWORD"),
                0,
                List.of("PDF", "pdf", "Docx"),
                true,
                -5);

        assertThat(settings.maxFileSizeBytes()).isEqualTo(20 * 1024 * 1024);
        assertThat(settings.allowedFileExtensions()).containsExactly("pdf", "docx");
        assertThat(settings.fileRetentionDays()).isZero();
    }

    @Test
    void whenAllowedExtensionsNull_thenDefaultsToEmptyImmutableList() {
        PolicyToggleSettings settings = new PolicyToggleSettings(true, true, false,
                List.of(),
                4096L,
                null,
                false,
                10);

        assertThat(settings.allowedFileExtensions()).isEmpty();
        assertThatThrownBy(() -> settings.allowedFileExtensions().add("txt"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void auditFields_haveSecureDefaultsAndNormalization() {
        PolicyToggleSettings settings = new PolicyToggleSettings(true, true, false,
                List.of("PASSWORD"),
                1024L,
                List.of("pdf"),
                true,
                30,
                false,
                false,
                true,
                -1,
                false,
                "high",
                true,
                List.of("/api/accounts/**"),
                List.of("AUDIT_ADMIN"));

        assertThat(settings.auditEnabled()).isFalse();
        assertThat(settings.auditReasonRequired()).isFalse();
        assertThat(settings.auditSensitiveApiDefaultOn()).isTrue();
        assertThat(settings.auditRetentionDays()).isZero();
        assertThat(settings.auditStrictMode()).isFalse();
        assertThat(settings.auditRiskLevel()).isEqualTo("HIGH");
        assertThat(settings.auditMaskingEnabled()).isTrue();
        assertThat(settings.auditSensitiveEndpoints()).contains("/api/accounts/**");
        assertThat(settings.auditUnmaskRoles()).contains("AUDIT_ADMIN");
    }

    @Test
    void cronFields_nullOrBlankDefaultsToDefaults() {
        PolicyToggleSettings settings = new PolicyToggleSettings(
                true, true, false,
                List.of("PASSWORD"),
                1024L,
                List.of("pdf"),
                true,
                30,
                true, true, true, 730, true, null, true, null, null,
                true, null, -1,
                true, "   ",
                true, "",
                true, null,
                true, "   ",
                null
        );

        assertThat(settings.auditRiskLevel()).isEqualTo("MEDIUM");
        assertThat(settings.auditSensitiveEndpoints()).isEmpty();
        assertThat(settings.auditUnmaskRoles()).isEmpty();
        assertThat(settings.auditPartitionCron()).isEqualTo("0 0 2 1 * *");
        assertThat(settings.auditPartitionPreloadMonths()).isZero();
        assertThat(settings.auditMonthlyReportCron()).isEqualTo("0 0 4 1 * *");
        assertThat(settings.auditLogRetentionCron()).isEqualTo("0 0 3 * * *");
        assertThat(settings.auditColdArchiveCron()).isEqualTo("0 30 2 2 * *");
        assertThat(settings.auditRetentionCleanupCron()).isEqualTo("0 30 3 * * *");
        assertThat(settings.batchJobs()).isNotNull();
    }

    @Test
    void partitionFieldsAndBatchJobs_normalValues() {
        java.util.Map<com.example.common.schedule.BatchJobCode, com.example.common.schedule.BatchJobSchedule> customJobs =
                java.util.Map.of(
                        com.example.common.schedule.BatchJobCode.AUDIT_MONTHLY_REPORT,
                        new com.example.common.schedule.BatchJobSchedule(
                                true,
                                com.example.common.schedule.TriggerType.CRON,
                                "0 0 5 1 * *",
                                0L,
                                0L,
                                "UTC"
                        )
                );
        PolicyToggleSettings settings = new PolicyToggleSettings(
                true, true, false,
                List.of("PASSWORD"),
                1024L,
                List.of("pdf"),
                true,
                30,
                true, true, true, 730, true, "LOW", true, List.of("/api/test"), List.of("ADMIN"),
                true, "0 0 1 1 * *", 3,
                true, "0 0 2 1 * *",
                true, "0 0 2 * * *",
                true, "0 0 1 2 * *",
                true, "0 0 4 * * *",
                customJobs
        );

        assertThat(settings.auditPartitionCron()).isEqualTo("0 0 1 1 * *");
        assertThat(settings.auditPartitionPreloadMonths()).isEqualTo(3);
        assertThat(settings.auditMonthlyReportCron()).isEqualTo("0 0 2 1 * *");
        assertThat(settings.auditLogRetentionCron()).isEqualTo("0 0 2 * * *");
        assertThat(settings.auditColdArchiveCron()).isEqualTo("0 0 1 2 * *");
        assertThat(settings.auditRetentionCleanupCron()).isEqualTo("0 0 4 * * *");
        assertThat(settings.batchJobs()).containsKey(com.example.common.schedule.BatchJobCode.AUDIT_MONTHLY_REPORT);
    }

    @Test
    void compatibilityConstructor_withAuditFieldsOnly() {
        PolicyToggleSettings settings = new PolicyToggleSettings(
                true, true, false,
                List.of("PASSWORD"),
                1024L,
                List.of("pdf"),
                true,
                30,
                true, true, true, 365, true, "HIGH", true, List.of("/api/admin"), List.of("SUPER_ADMIN"),
                false, "0 0 3 1 * *", 2,
                true, "0 0 5 1 * *",
                true, "0 0 4 * * *",
                false, "0 0 3 2 * *",
                true, "0 0 5 * * *"
        );

        assertThat(settings.batchJobs()).isNotNull();
        assertThat(settings.batchJobs()).isNotEmpty();
    }
}
