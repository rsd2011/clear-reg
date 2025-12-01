package com.example.server.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.admin.policy.dto.PolicyUpdateRequest;
import com.example.admin.policy.dto.PolicyYamlRequest;
import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.admin.systemconfig.dto.SystemConfigDraftRequest;
import com.example.admin.systemconfig.dto.SystemConfigRootRequest;
import com.example.admin.systemconfig.dto.SystemConfigRootResponse;
import com.example.admin.systemconfig.dto.settings.AuditSettings;
import com.example.admin.systemconfig.dto.settings.AuthenticationSettings;
import com.example.admin.systemconfig.dto.settings.FileUploadSettings;
import com.example.admin.systemconfig.service.SystemConfigPolicySettingsProvider;
import com.example.admin.systemconfig.service.SystemConfigSettingsParser;
import com.example.admin.systemconfig.service.SystemConfigVersioningService;
import com.example.audit.AuditPort;
import com.example.common.policy.PolicyToggleSettings;
import com.example.common.schedule.BatchJobCode;
import com.example.common.schedule.BatchJobDefaults;
import com.example.common.schedule.BatchJobSchedule;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SystemConfigPolicyAdminPortAdapter 테스트")
class SystemConfigPolicyAdminPortAdapterTest {

    @Mock
    private SystemConfigVersioningService versioningService;

    @Mock
    private SystemConfigPolicySettingsProvider settingsProvider;

    @Mock
    private SystemConfigSettingsParser parser;

    @Mock
    private AuditPort auditPort;

    private SystemConfigPolicyAdminPortAdapter adapter;

    private static final UUID CONFIG_ID = UUID.randomUUID();
    private static final String TEST_USER = "testUser";

    @BeforeEach
    void setUp() {
        adapter = new SystemConfigPolicyAdminPortAdapter(
                versioningService, settingsProvider, parser, auditPort
        );
        AuthContextHolder.set(AuthContext.of(TEST_USER, "ORG001", "ADMIN",
                FeatureCode.POLICY, ActionCode.UPDATE));
    }

    @Nested
    @DisplayName("currentPolicy")
    class CurrentPolicy {

        @Test
        @DisplayName("현재 정책 설정을 조회할 수 있다")
        void returnsCurrentPolicy() {
            // given
            var authSettings = AuthenticationSettings.defaults();
            var fileSettings = FileUploadSettings.defaults();
            var auditSettings = AuditSettings.defaults();
            var batchJobs = BatchJobDefaults.defaults();

            given(settingsProvider.getAuthSettings()).willReturn(Optional.of(authSettings));
            given(settingsProvider.getFileSettings()).willReturn(Optional.of(fileSettings));
            given(settingsProvider.getAuditSettings()).willReturn(Optional.of(auditSettings));
            given(settingsProvider.currentSettings()).willReturn(createPolicyToggleSettings(batchJobs));
            lenient().when(parser.toYaml(any(AuthenticationSettings.class))).thenReturn("auth: yaml");
            lenient().when(parser.toYaml(any(FileUploadSettings.class))).thenReturn("file: yaml");
            lenient().when(parser.toYaml(any(AuditSettings.class))).thenReturn("audit: yaml");

            // when
            var result = adapter.currentPolicy();

            // then
            assertThat(result).isNotNull();
            assertThat(result.passwordPolicyEnabled()).isEqualTo(authSettings.passwordPolicyEnabled());
            assertThat(result.auditEnabled()).isEqualTo(auditSettings.auditEnabled());
        }

        @Test
        @DisplayName("설정이 없으면 기본값을 사용한다")
        void usesDefaultsWhenSettingsNotFound() {
            // given
            given(settingsProvider.getAuthSettings()).willReturn(Optional.empty());
            given(settingsProvider.getFileSettings()).willReturn(Optional.empty());
            given(settingsProvider.getAuditSettings()).willReturn(Optional.empty());
            given(settingsProvider.currentSettings()).willReturn(createPolicyToggleSettings(BatchJobDefaults.defaults()));
            lenient().when(parser.toYaml(any(AuthenticationSettings.class))).thenReturn("auth: yaml");
            lenient().when(parser.toYaml(any(FileUploadSettings.class))).thenReturn("file: yaml");
            lenient().when(parser.toYaml(any(AuditSettings.class))).thenReturn("audit: yaml");

            // when
            var result = adapter.currentPolicy();

            // then
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("updateToggles")
    class UpdateToggles {

        @Test
        @DisplayName("인증 설정을 업데이트할 수 있다")
        void updatesAuthSettings() {
            // given
            setupCurrentSettings();
            var existingConfig = createRootResponse("auth.settings");
            given(versioningService.findByConfigCode("auth.settings")).willReturn(Optional.of(existingConfig));
            lenient().when(versioningService.findByConfigCode("file.settings")).thenReturn(Optional.empty());
            lenient().when(versioningService.findByConfigCode("audit.settings")).thenReturn(Optional.empty());

            // passwordPolicyEnabled를 false로 변경하는 요청
            var request = createPolicyUpdateRequestForAuth(false);

            lenient().when(parser.toYaml(any(AuthenticationSettings.class))).thenReturn("auth: updated");
            lenient().when(parser.toYaml(any(FileUploadSettings.class))).thenReturn("file: yaml");
            lenient().when(parser.toYaml(any(AuditSettings.class))).thenReturn("audit: yaml");

            // when
            var result = adapter.updateToggles(request);

            // then
            verify(versioningService).updateConfig(
                    org.mockito.ArgumentMatchers.eq(CONFIG_ID),
                    any(SystemConfigDraftRequest.class),
                    any(AuthContext.class)
            );
            verify(settingsProvider).refreshCache();
        }

        @Test
        @DisplayName("감사 설정을 업데이트할 수 있다")
        void updatesAuditSettings() {
            // given
            setupCurrentSettings();
            var existingConfig = createRootResponse("audit.settings");
            lenient().when(versioningService.findByConfigCode("auth.settings")).thenReturn(Optional.empty());
            lenient().when(versioningService.findByConfigCode("file.settings")).thenReturn(Optional.empty());
            given(versioningService.findByConfigCode("audit.settings")).willReturn(Optional.of(existingConfig));

            // auditEnabled를 false로 변경하는 요청
            var request = createPolicyUpdateRequestForAudit(false, 180);

            lenient().when(parser.toYaml(any(AuthenticationSettings.class))).thenReturn("auth: yaml");
            lenient().when(parser.toYaml(any(FileUploadSettings.class))).thenReturn("file: yaml");
            lenient().when(parser.toYaml(any(AuditSettings.class))).thenReturn("audit: updated");

            // when
            var result = adapter.updateToggles(request);

            // then
            verify(versioningService).updateConfig(
                    org.mockito.ArgumentMatchers.eq(CONFIG_ID),
                    any(SystemConfigDraftRequest.class),
                    any(AuthContext.class)
            );
        }

        @Test
        @DisplayName("설정이 없으면 새로 생성한다")
        void createsNewConfigWhenNotExists() {
            // given
            setupCurrentSettings();
            given(versioningService.findByConfigCode("auth.settings")).willReturn(Optional.empty());
            lenient().when(versioningService.findByConfigCode("file.settings")).thenReturn(Optional.empty());
            lenient().when(versioningService.findByConfigCode("audit.settings")).thenReturn(Optional.empty());

            var request = createPolicyUpdateRequestForAuth(false);

            lenient().when(parser.toYaml(any(AuthenticationSettings.class))).thenReturn("auth: new");
            lenient().when(parser.toYaml(any(FileUploadSettings.class))).thenReturn("file: yaml");
            lenient().when(parser.toYaml(any(AuditSettings.class))).thenReturn("audit: yaml");

            // when
            adapter.updateToggles(request);

            // then
            verify(versioningService).createConfig(any(SystemConfigRootRequest.class), any(AuthContext.class));
        }
    }

    @Nested
    @DisplayName("updateFromYaml")
    class UpdateFromYaml {

        @Test
        @DisplayName("YAML로 정책을 업데이트할 수 있다")
        void updatesFromYaml() {
            // given
            setupCurrentSettings();
            var existingConfig = createRootResponse("audit.settings");
            given(versioningService.findByConfigCode("audit.settings")).willReturn(Optional.of(existingConfig));
            given(parser.parseAuditSettings(any())).willReturn(AuditSettings.defaults());
            lenient().when(parser.toYaml(any(AuthenticationSettings.class))).thenReturn("auth: yaml");
            lenient().when(parser.toYaml(any(FileUploadSettings.class))).thenReturn("file: yaml");
            lenient().when(parser.toYaml(any(AuditSettings.class))).thenReturn("audit: yaml");

            var request = new PolicyYamlRequest("audit:\n  enabled: true");

            // when
            var result = adapter.updateFromYaml(request);

            // then
            verify(versioningService).updateConfig(
                    org.mockito.ArgumentMatchers.eq(CONFIG_ID),
                    any(SystemConfigDraftRequest.class),
                    any(AuthContext.class)
            );
            verify(settingsProvider).refreshCache();
        }
    }

    // === Helper Methods ===

    private void setupCurrentSettings() {
        given(settingsProvider.getAuthSettings()).willReturn(Optional.of(AuthenticationSettings.defaults()));
        given(settingsProvider.getFileSettings()).willReturn(Optional.of(FileUploadSettings.defaults()));
        given(settingsProvider.getAuditSettings()).willReturn(Optional.of(AuditSettings.defaults()));
        given(settingsProvider.currentSettings()).willReturn(createPolicyToggleSettings(BatchJobDefaults.defaults()));
    }

    private SystemConfigRootResponse createRootResponse(String configCode) {
        return new SystemConfigRootResponse(
                CONFIG_ID, configCode, configCode, "설명",
                OffsetDateTime.now(), OffsetDateTime.now(),
                1, false, true
        );
    }

    private PolicyToggleSettings createPolicyToggleSettings(
            Map<BatchJobCode, BatchJobSchedule> batchJobs) {
        return new PolicyToggleSettings(
                true, true, true, List.of("ID_PASSWORD"),
                10485760L, List.of("pdf", "doc"), true, 365,
                true, false, false, 365, false, "MEDIUM",
                true, List.of(), List.of("ADMIN"),
                true, "0 0 1 * * ?", 3,
                true, "0 0 2 1 * ?",
                true, "0 0 3 * * ?",
                false, "0 0 4 * * ?",
                true, "0 0 5 * * ?",
                batchJobs
        );
    }

    /**
     * 인증 설정 변경용 PolicyUpdateRequest 생성
     */
    private PolicyUpdateRequest createPolicyUpdateRequestForAuth(Boolean passwordPolicyEnabled) {
        return new PolicyUpdateRequest(
                passwordPolicyEnabled, null, null, null,  // auth settings (passwordPolicyEnabled만 설정)
                null, null, null, null,  // file settings (all null)
                null, null, null, null,  // audit settings part 1 (all null)
                null, null, null, null, null,  // audit settings part 2
                null, null, null, null, null, null, null,  // partition settings
                null, null, null, null, null, null, null, null, null  // remaining fields
        );
    }

    /**
     * 감사 설정 변경용 PolicyUpdateRequest 생성
     */
    private PolicyUpdateRequest createPolicyUpdateRequestForAudit(Boolean auditEnabled, Integer auditRetentionDays) {
        return new PolicyUpdateRequest(
                null, null, null, null,  // auth settings (all null)
                null, null, null, null,  // file settings (all null)
                auditEnabled, null, null, auditRetentionDays,  // audit settings (auditEnabled, auditRetentionDays 설정)
                null, null, null, null, null,  // audit settings part 2
                null, null, null, null, null, null, null,  // partition settings
                null, null, null, null, null, null, null, null, null  // remaining fields
        );
    }
}
