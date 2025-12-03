package com.example.batch.worker.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;

class DwWorkerPolicyConfigTest {

    private final DwWorkerPolicyConfig config = new DwWorkerPolicyConfig();

    @Test
    @DisplayName("기본 정책 설정이 주입되지 않은 경우 워커 기본값을 제공한다")
    void providesDefaultPolicySettingsWhenMissingBean() {
        // Given: 외부에서 PolicySettingsProvider 빈이 정의되지 않았을 때

        // When: 구성 클래스가 기본 빈을 생성하면
        PolicySettingsProvider provider = config.policySettingsProvider();
        PolicyToggleSettings settings = provider.currentSettings();

        // Then: 파일 제한, 민감태그, 보관일수 등의 기본값이 설정된다
        assertThat(settings.passwordPolicyEnabled()).isFalse();
        assertThat(settings.passwordHistoryEnabled()).isFalse();
        assertThat(settings.maxFileSizeBytes()).isEqualTo(20 * 1024 * 1024L);
        assertThat(settings.allowedFileExtensions()).contains("pdf", "csv", "txt");
        assertThat(settings.fileRetentionDays()).isEqualTo(365);
    }
}
