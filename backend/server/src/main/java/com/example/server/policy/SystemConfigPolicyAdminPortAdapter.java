package com.example.server.policy;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.admin.policy.dto.PolicyUpdateRequest;
import com.example.admin.policy.dto.PolicyView;
import com.example.admin.policy.dto.PolicyYamlRequest;
import com.example.admin.systemconfig.dto.SystemConfigDraftRequest;
import com.example.admin.systemconfig.dto.SystemConfigRootRequest;
import com.example.admin.systemconfig.dto.settings.AuditSettings;
import com.example.admin.systemconfig.dto.settings.AuthenticationSettings;
import com.example.admin.systemconfig.dto.settings.FileUploadSettings;
import com.example.admin.systemconfig.service.SystemConfigPolicySettingsProvider;
import com.example.admin.systemconfig.service.SystemConfigSettingsParser;
import com.example.admin.systemconfig.service.SystemConfigVersioningService;
import com.example.audit.Actor;
import com.example.audit.ActorType;
import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPort;
import com.example.audit.RiskLevel;
import com.example.common.schedule.BatchJobCode;
import com.example.common.schedule.BatchJobSchedule;

/**
 * SystemConfig 기반 PolicyAdminPort 구현.
 * <p>
 * 활성화 조건:
 * <ul>
 *   <li>{@code clearreg.policy.provider=systemconfig} (기본값)</li>
 *   <li>SystemConfigVersioningService 빈이 존재해야 함</li>
 * </ul>
 * </p>
 */
@Component
@ConditionalOnProperty(
    name = "clearreg.policy.provider",
    havingValue = "systemconfig",
    matchIfMissing = true
)
@ConditionalOnBean(SystemConfigVersioningService.class)
public class SystemConfigPolicyAdminPortAdapter implements PolicyAdminPort {

    private static final String AUTH_CONFIG_CODE = SystemConfigSettingsParser.AUTH_CONFIG_CODE;
    private static final String FILE_CONFIG_CODE = SystemConfigSettingsParser.FILE_CONFIG_CODE;
    private static final String AUDIT_CONFIG_CODE = SystemConfigSettingsParser.AUDIT_CONFIG_CODE;

    private final SystemConfigVersioningService versioningService;
    private final SystemConfigPolicySettingsProvider settingsProvider;
    private final SystemConfigSettingsParser parser;
    private final AuditPort auditPort;

    public SystemConfigPolicyAdminPortAdapter(
            SystemConfigVersioningService versioningService,
            SystemConfigPolicySettingsProvider settingsProvider,
            SystemConfigSettingsParser parser,
            AuditPort auditPort) {
        this.versioningService = versioningService;
        this.settingsProvider = settingsProvider;
        this.parser = parser;
        this.auditPort = auditPort;
    }

    @Override
    public PolicyView currentPolicy() {
        AuthenticationSettings auth = settingsProvider.getAuthSettings()
                .orElse(AuthenticationSettings.defaults());
        FileUploadSettings file = settingsProvider.getFileSettings()
                .orElse(FileUploadSettings.defaults());
        AuditSettings audit = settingsProvider.getAuditSettings()
                .orElse(AuditSettings.defaults());
        Map<BatchJobCode, BatchJobSchedule> batchJobs = settingsProvider.currentSettings().batchJobs();

        return toPolicyView(auth, file, audit, batchJobs);
    }

    @Override
    public PolicyView updateToggles(PolicyUpdateRequest request) {
        PolicyView before = currentPolicy();

        // 각 설정을 개별적으로 업데이트
        updateAuthSettings(request);
        updateFileSettings(request);
        updateAuditSettings(request);

        // 캐시 갱신
        settingsProvider.refreshCache();

        PolicyView after = currentPolicy();
        recordPolicyChange("UPDATE_TOGGLES", before, after);
        return after;
    }

    @Override
    public PolicyView updateFromYaml(PolicyYamlRequest request) {
        PolicyView before = currentPolicy();

        // YAML을 파싱하여 각 설정 타입으로 분리하고 업데이트
        // 전체 YAML은 audit.settings로 저장 (호환성 유지)
        String yaml = request.yaml();
        AuditSettings auditSettings = parser.parseAuditSettings(yaml);

        updateOrCreateConfig(AUDIT_CONFIG_CODE, yaml, "정책 YAML 업데이트");

        // 캐시 갱신
        settingsProvider.refreshCache();

        PolicyView after = currentPolicy();
        recordPolicyChange("UPDATE_YAML", before, after);
        return after;
    }

    private void updateAuthSettings(PolicyUpdateRequest request) {
        AuthenticationSettings current = settingsProvider.getAuthSettings()
                .orElse(AuthenticationSettings.defaults());

        AuthenticationSettings updated = new AuthenticationSettings(
                request.passwordPolicyEnabled() != null ? request.passwordPolicyEnabled() : current.passwordPolicyEnabled(),
                request.passwordHistoryEnabled() != null ? request.passwordHistoryEnabled() : current.passwordHistoryEnabled(),
                current.passwordHistoryCount(),
                request.accountLockEnabled() != null ? request.accountLockEnabled() : current.accountLockEnabled(),
                current.accountLockThreshold(),
                current.accountLockDurationMinutes(),
                request.enabledLoginTypes() != null ? request.enabledLoginTypes() : current.enabledLoginTypes(),
                current.sessionTimeoutMinutes(),
                current.concurrentSessionAllowed(),
                current.maxConcurrentSessions(),
                current.ssoEnabled(),
                current.ssoProvider()
        );

        if (!updated.equals(current)) {
            String yaml = parser.toYaml(updated);
            updateOrCreateConfig(AUTH_CONFIG_CODE, yaml, "인증 설정 업데이트");
        }
    }

    private void updateFileSettings(PolicyUpdateRequest request) {
        FileUploadSettings current = settingsProvider.getFileSettings()
                .orElse(FileUploadSettings.defaults());

        FileUploadSettings updated = new FileUploadSettings(
                request.maxFileSizeBytes() != null ? request.maxFileSizeBytes() : current.maxFileSizeBytes(),
                request.allowedFileExtensions() != null
                        ? request.allowedFileExtensions().stream().map(String::toLowerCase).toList()
                        : current.allowedFileExtensions(),
                request.strictMimeValidation() != null ? request.strictMimeValidation() : current.strictMimeValidation(),
                request.fileRetentionDays() != null ? request.fileRetentionDays() : current.fileRetentionDays(),
                current.virusScanEnabled(),
                current.imageResizeEnabled(),
                current.imageMaxWidth(),
                current.imageMaxHeight(),
                current.thumbnailGenerationEnabled(),
                current.thumbnailSize(),
                current.tempFileCleanupHours(),
                current.storageType()
        );

        if (!updated.equals(current)) {
            String yaml = parser.toYaml(updated);
            updateOrCreateConfig(FILE_CONFIG_CODE, yaml, "파일 설정 업데이트");
        }
    }

    private void updateAuditSettings(PolicyUpdateRequest request) {
        AuditSettings current = settingsProvider.getAuditSettings()
                .orElse(AuditSettings.defaults());

        AuditSettings updated = new AuditSettings(
                request.auditEnabled() != null ? request.auditEnabled() : current.auditEnabled(),
                request.auditReasonRequired() != null ? request.auditReasonRequired() : current.auditReasonRequired(),
                request.auditSensitiveApiDefaultOn() != null ? request.auditSensitiveApiDefaultOn() : current.auditSensitiveApiDefaultOn(),
                request.auditRetentionDays() != null ? request.auditRetentionDays() : current.auditRetentionDays(),
                request.auditStrictMode() != null ? request.auditStrictMode() : current.auditStrictMode(),
                request.auditRiskLevel() != null ? request.auditRiskLevel() : current.auditRiskLevel(),
                request.auditMaskingEnabled() != null ? request.auditMaskingEnabled() : current.auditMaskingEnabled(),
                request.auditSensitiveEndpoints() != null ? request.auditSensitiveEndpoints() : current.auditSensitiveEndpoints(),
                request.auditUnmaskRoles() != null ? request.auditUnmaskRoles() : current.auditUnmaskRoles(),
                request.auditPartitionEnabled() != null ? request.auditPartitionEnabled() : current.auditPartitionEnabled(),
                request.auditPartitionCron() != null ? request.auditPartitionCron() : current.auditPartitionCron(),
                request.auditPartitionPreloadMonths() != null ? request.auditPartitionPreloadMonths() : current.auditPartitionPreloadMonths(),
                request.auditMonthlyReportEnabled() != null ? request.auditMonthlyReportEnabled() : current.auditMonthlyReportEnabled(),
                request.auditMonthlyReportCron() != null ? request.auditMonthlyReportCron() : current.auditMonthlyReportCron(),
                request.auditLogRetentionEnabled() != null ? request.auditLogRetentionEnabled() : current.auditLogRetentionEnabled(),
                request.auditLogRetentionCron() != null ? request.auditLogRetentionCron() : current.auditLogRetentionCron(),
                request.auditColdArchiveEnabled() != null ? request.auditColdArchiveEnabled() : current.auditColdArchiveEnabled(),
                request.auditColdArchiveCron() != null ? request.auditColdArchiveCron() : current.auditColdArchiveCron(),
                request.auditRetentionCleanupEnabled() != null ? request.auditRetentionCleanupEnabled() : current.auditRetentionCleanupEnabled(),
                request.auditRetentionCleanupCron() != null ? request.auditRetentionCleanupCron() : current.auditRetentionCleanupCron()
        );

        if (!updated.equals(current)) {
            String yaml = parser.toYaml(updated);
            updateOrCreateConfig(AUDIT_CONFIG_CODE, yaml, "감사 설정 업데이트");
        }
    }

    private void updateOrCreateConfig(String configCode, String yaml, String changeReason) {
        AuthContext ctx = AuthContextHolder.current().orElse(null);

        var existingConfig = versioningService.findByConfigCode(configCode);

        if (existingConfig.isPresent()) {
            versioningService.updateConfig(
                    existingConfig.get().id(),
                    new SystemConfigDraftRequest(yaml, true, changeReason),
                    ctx
            );
        } else {
            versioningService.createConfig(
                    new SystemConfigRootRequest(
                            configCode,
                            configCode,
                            "자동 생성된 정책 설정",
                            yaml,
                            true
                    ),
                    ctx
            );
        }
    }

    private PolicyView toPolicyView(
            AuthenticationSettings auth,
            FileUploadSettings file,
            AuditSettings audit,
            Map<BatchJobCode, BatchJobSchedule> batchJobs) {

        // 전체 YAML 생성
        String yaml = generateCombinedYaml(auth, file, audit);

        return new PolicyView(
                auth.passwordPolicyEnabled(),
                auth.passwordHistoryEnabled(),
                auth.accountLockEnabled(),
                auth.enabledLoginTypes(),
                file.maxFileSizeBytes(),
                file.allowedFileExtensions(),
                file.strictMimeValidation(),
                file.fileRetentionDays(),
                audit.auditEnabled(),
                audit.auditReasonRequired(),
                audit.auditSensitiveApiDefaultOn(),
                audit.auditRetentionDays(),
                audit.auditStrictMode(),
                audit.auditRiskLevel(),
                audit.auditMaskingEnabled(),
                audit.auditSensitiveEndpoints(),
                audit.auditUnmaskRoles(),
                audit.auditPartitionEnabled(),
                audit.auditPartitionCron(),
                audit.auditPartitionPreloadMonths(),
                "", // auditPartitionTablespaceHot
                "", // auditPartitionTablespaceCold
                6,  // auditPartitionHotMonths
                60, // auditPartitionColdMonths
                audit.auditMonthlyReportEnabled(),
                audit.auditMonthlyReportCron(),
                audit.auditLogRetentionEnabled(),
                audit.auditLogRetentionCron(),
                audit.auditColdArchiveEnabled(),
                audit.auditColdArchiveCron(),
                audit.auditRetentionCleanupEnabled(),
                audit.auditRetentionCleanupCron(),
                batchJobs,
                yaml
        );
    }

    private String generateCombinedYaml(
            AuthenticationSettings auth,
            FileUploadSettings file,
            AuditSettings audit) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Authentication Settings\n");
        sb.append(parser.toYaml(auth));
        sb.append("\n# File Upload Settings\n");
        sb.append(parser.toYaml(file));
        sb.append("\n# Audit Settings\n");
        sb.append(parser.toYaml(audit));
        return sb.toString();
    }

    private void recordPolicyChange(String action, PolicyView before, PolicyView after) {
        var ctx = AuthContextHolder.current().orElse(null);
        AuditEvent event = AuditEvent.builder()
                .eventType("POLICY_CHANGE")
                .moduleName("policy")
                .action(action)
                .actor(Actor.builder()
                        .id(ctx != null ? ctx.username() : "unknown")
                        .role(ctx != null ? ctx.permissionGroupCode() : null)
                        .type(ActorType.HUMAN)
                        .dept(ctx != null ? ctx.organizationCode() : null)
                        .build())
                .subject(com.example.audit.Subject.builder()
                        .type("SYSTEM_CONFIG")
                        .key("policy")
                        .build())
                .success(true)
                .resultCode("OK")
                .riskLevel(RiskLevel.MEDIUM)
                .beforeSummary(Integer.toString(before.hashCode()))
                .afterSummary(Integer.toString(after.hashCode()))
                .extraEntry("beforeYaml", before.yaml())
                .extraEntry("afterYaml", after.yaml())
                .build();
        try {
            auditPort.record(event, AuditMode.ASYNC_FALLBACK);
        } catch (Exception ex) {
            // 정책 변경 실패로 이어지지 않도록 예외 삼킴
        }
    }
}
