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
}
