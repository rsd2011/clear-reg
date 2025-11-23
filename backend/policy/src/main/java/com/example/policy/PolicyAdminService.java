package com.example.policy;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;
import com.example.policy.dto.PolicyUpdateRequest;
import com.example.policy.dto.PolicyView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PolicyAdminService {

    private static final String DOCUMENT_CODE = "security.policy";

    private final PolicyDocumentRepository repository;
    private final ObjectMapper yamlMapper;
    private final AtomicReference<PolicyState> cache;

    public PolicyAdminService(PolicyDocumentRepository repository,
                              @Qualifier("yamlObjectMapper") ObjectMapper yamlMapper,
                              PolicyToggleSettings defaults) {
        this.repository = repository;
        this.yamlMapper = yamlMapper.copy();
        PolicyState initial = PolicyState.from(defaults);
        this.cache = new AtomicReference<>(initial);
        repository.findByCode(DOCUMENT_CODE)
                .ifPresent(doc -> this.cache.set(readState(doc.getYaml())));
    }

    public PolicyState currentState() {
        return cache.get();
    }

    public PolicySnapshot snapshot() {
        PolicyState state = cache.get();
        return new PolicySnapshot(state, exportYaml(state));
    }

    public PolicyView currentView() {
        return toView(snapshot());
    }

    @Transactional
    public PolicySnapshot update(PolicyUpdateRequest request) {
        PolicyState updated = cache.updateAndGet(current -> current.merge(request));
        persist(updated);
        return new PolicySnapshot(updated, exportYaml(updated));
    }

    @Transactional
    public PolicyView updateView(PolicyUpdateRequest request) {
        return toView(update(request));
    }

    @Transactional
    public PolicySnapshot applyYaml(String yaml) {
        PolicyState state = readState(yaml);
        cache.set(state);
        persist(state, yaml);
        return new PolicySnapshot(state, yaml);
    }

    @Transactional
    public PolicyView applyYamlView(String yaml) {
        return toView(applyYaml(yaml));
    }

    private void persist(PolicyState state) {
        persist(state, exportYaml(state));
    }

    private void persist(PolicyState state, String yaml) {
        PolicyDocument document = repository.findByCode(DOCUMENT_CODE)
                .orElseGet(() -> new PolicyDocument(DOCUMENT_CODE, yaml));
        document.updateYaml(yaml);
        repository.save(document);
    }

    private PolicyState readState(String yaml) {
        try {
            PolicyToggleSettings settings = yamlMapper.readValue(yaml, PolicyToggleSettings.class);
            return PolicyState.from(settings);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid policy YAML", exception);
        }
    }

    private String exportYaml(PolicyState state) {
        try {
            return yamlMapper.writeValueAsString(state.toSettings());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to export policy YAML", exception);
        }
    }

    public record PolicySnapshot(PolicyState state, String yaml) {
    }

    private PolicyView toView(PolicySnapshot snapshot) {
        PolicyState state = snapshot.state();
        return new PolicyView(state.passwordPolicyEnabled(),
                state.passwordHistoryEnabled(),
                state.accountLockEnabled(),
                state.enabledLoginTypes(),
                state.maxFileSizeBytes(),
                state.allowedFileExtensions(),
                state.strictMimeValidation(),
                state.fileRetentionDays(),
                state.auditEnabled(),
                state.auditReasonRequired(),
                state.auditSensitiveApiDefaultOn(),
                state.auditRetentionDays(),
                state.auditStrictMode(),
                state.auditRiskLevel(),
                state.auditMaskingEnabled(),
                state.auditSensitiveEndpoints(),
                state.auditUnmaskRoles(),
                state.auditPartitionEnabled(),
                state.auditPartitionCron(),
                state.auditPartitionPreloadMonths(),
                state.auditMonthlyReportEnabled(),
                state.auditMonthlyReportCron(),
                snapshot.yaml());
    }

    public static final class PolicyState {

        private final boolean passwordPolicyEnabled;
        private final boolean passwordHistoryEnabled;
        private final boolean accountLockEnabled;
        private final List<String> enabledLoginTypes;
        private final long maxFileSizeBytes;
        private final List<String> allowedFileExtensions;
        private final boolean strictMimeValidation;
        private final int fileRetentionDays;
        private final boolean auditEnabled;
        private final boolean auditReasonRequired;
        private final boolean auditSensitiveApiDefaultOn;
        private final int auditRetentionDays;
        private final boolean auditStrictMode;
        private final String auditRiskLevel;
        private final boolean auditMaskingEnabled;
        private final List<String> auditSensitiveEndpoints;
        private final List<String> auditUnmaskRoles;
        private final boolean auditPartitionEnabled;
        private final String auditPartitionCron;
        private final int auditPartitionPreloadMonths;
        private final boolean auditMonthlyReportEnabled;
        private final String auditMonthlyReportCron;

        private PolicyState(boolean passwordPolicyEnabled,
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
                            boolean auditMonthlyReportEnabled,
                            String auditMonthlyReportCron) {
            this.passwordPolicyEnabled = passwordPolicyEnabled;
            this.passwordHistoryEnabled = passwordHistoryEnabled;
            this.accountLockEnabled = accountLockEnabled;
            this.enabledLoginTypes = List.copyOf(enabledLoginTypes);
            this.maxFileSizeBytes = maxFileSizeBytes;
            this.allowedFileExtensions = List.copyOf(allowedFileExtensions == null ? List.of() : allowedFileExtensions);
            this.strictMimeValidation = strictMimeValidation;
            this.fileRetentionDays = fileRetentionDays;
            this.auditEnabled = auditEnabled;
            this.auditReasonRequired = auditReasonRequired;
            this.auditSensitiveApiDefaultOn = auditSensitiveApiDefaultOn;
            this.auditRetentionDays = auditRetentionDays;
            this.auditStrictMode = auditStrictMode;
            this.auditRiskLevel = auditRiskLevel;
            this.auditMaskingEnabled = auditMaskingEnabled;
            this.auditSensitiveEndpoints = List.copyOf(auditSensitiveEndpoints == null ? List.of() : auditSensitiveEndpoints);
            this.auditUnmaskRoles = List.copyOf(auditUnmaskRoles == null ? List.of() : auditUnmaskRoles);
            this.auditPartitionEnabled = auditPartitionEnabled;
            this.auditPartitionCron = auditPartitionCron;
            this.auditPartitionPreloadMonths = auditPartitionPreloadMonths;
            this.auditMonthlyReportEnabled = auditMonthlyReportEnabled;
            this.auditMonthlyReportCron = auditMonthlyReportCron;
        }

        public static PolicyState from(PolicyToggleSettings settings) {
            return new PolicyState(settings.passwordPolicyEnabled(),
                    settings.passwordHistoryEnabled(),
                    settings.accountLockEnabled(),
                    settings.enabledLoginTypes(),
                    settings.maxFileSizeBytes(),
                    settings.allowedFileExtensions(),
                    settings.strictMimeValidation(),
                    settings.fileRetentionDays(),
                    settings.auditEnabled(),
                    settings.auditReasonRequired(),
                    settings.auditSensitiveApiDefaultOn(),
                    settings.auditRetentionDays(),
                    settings.auditStrictMode(),
                    settings.auditRiskLevel(),
                    settings.auditMaskingEnabled(),
                    settings.auditSensitiveEndpoints(),
                    settings.auditUnmaskRoles(),
                    settings.auditPartitionEnabled(),
                    settings.auditPartitionCron(),
                    settings.auditPartitionPreloadMonths(),
                    settings.auditMonthlyReportEnabled(),
                    settings.auditMonthlyReportCron());
        }

        public PolicyToggleSettings toSettings() {
            return new PolicyToggleSettings(passwordPolicyEnabled,
                    passwordHistoryEnabled,
                    accountLockEnabled,
                    enabledLoginTypes,
                    maxFileSizeBytes,
                    allowedFileExtensions,
                    strictMimeValidation,
                    fileRetentionDays,
                    auditEnabled,
                    auditReasonRequired,
                    auditSensitiveApiDefaultOn,
                    auditRetentionDays,
                    auditStrictMode,
                    auditRiskLevel,
                    auditMaskingEnabled,
                    auditSensitiveEndpoints,
                    auditUnmaskRoles);
        }

        public PolicyState merge(PolicyUpdateRequest request) {
            boolean newPasswordPolicyEnabled = request.passwordPolicyEnabled() != null ? request.passwordPolicyEnabled() : passwordPolicyEnabled;
            boolean newPasswordHistoryEnabled = request.passwordHistoryEnabled() != null ? request.passwordHistoryEnabled() : passwordHistoryEnabled;
            boolean newAccountLockEnabled = request.accountLockEnabled() != null ? request.accountLockEnabled() : accountLockEnabled;
            List<String> newTypes = request.enabledLoginTypes() != null ? request.enabledLoginTypes() : enabledLoginTypes;
            long newMaxFileSize = request.maxFileSizeBytes() != null && request.maxFileSizeBytes() > 0
                    ? request.maxFileSizeBytes()
                    : maxFileSizeBytes;
            List<String> newExtensions = request.allowedFileExtensions() != null
                    ? request.allowedFileExtensions().stream().map(String::toLowerCase).toList()
                    : allowedFileExtensions;
            boolean newStrictMime = request.strictMimeValidation() != null ? request.strictMimeValidation() : strictMimeValidation;
            int newRetention = request.fileRetentionDays() != null ? request.fileRetentionDays() : fileRetentionDays;
            boolean newAuditEnabled = request.auditEnabled() != null ? request.auditEnabled() : auditEnabled;
            boolean newAuditReasonRequired = request.auditReasonRequired() != null ? request.auditReasonRequired() : auditReasonRequired;
            boolean newAuditSensitiveApiDefaultOn = request.auditSensitiveApiDefaultOn() != null
                    ? request.auditSensitiveApiDefaultOn() : auditSensitiveApiDefaultOn;
            int newAuditRetention = request.auditRetentionDays() != null ? request.auditRetentionDays() : auditRetentionDays;
            boolean newAuditStrictMode = request.auditStrictMode() != null ? request.auditStrictMode() : auditStrictMode;
            String newAuditRiskLevel = request.auditRiskLevel() != null ? request.auditRiskLevel().toUpperCase() : auditRiskLevel;
            boolean newAuditMaskingEnabled = request.auditMaskingEnabled() != null ? request.auditMaskingEnabled() : auditMaskingEnabled;
            List<String> newAuditSensitiveEndpoints = request.auditSensitiveEndpoints() != null ? request.auditSensitiveEndpoints() : auditSensitiveEndpoints;
            List<String> newAuditUnmaskRoles = request.auditUnmaskRoles() != null ? request.auditUnmaskRoles() : auditUnmaskRoles;
            boolean newAuditPartitionEnabled = request.auditPartitionEnabled() != null ? request.auditPartitionEnabled() : auditPartitionEnabled;
            String newAuditPartitionCron = request.auditPartitionCron() != null ? request.auditPartitionCron() : auditPartitionCron;
            int newAuditPartitionPreloadMonths = request.auditPartitionPreloadMonths() != null ? request.auditPartitionPreloadMonths() : auditPartitionPreloadMonths;
            boolean newAuditMonthlyReportEnabled = request.auditMonthlyReportEnabled() != null ? request.auditMonthlyReportEnabled() : auditMonthlyReportEnabled;
            String newAuditMonthlyReportCron = request.auditMonthlyReportCron() != null ? request.auditMonthlyReportCron() : auditMonthlyReportCron;

            return new PolicyState(newPasswordPolicyEnabled, newPasswordHistoryEnabled, newAccountLockEnabled, newTypes,
                    newMaxFileSize, newExtensions, newStrictMime, Math.max(newRetention, 0),
                    newAuditEnabled, newAuditReasonRequired, newAuditSensitiveApiDefaultOn,
                    Math.max(newAuditRetention, 0), newAuditStrictMode, newAuditRiskLevel, newAuditMaskingEnabled, newAuditSensitiveEndpoints, newAuditUnmaskRoles,
                    newAuditPartitionEnabled, newAuditPartitionCron, Math.max(newAuditPartitionPreloadMonths, 0),
                    newAuditMonthlyReportEnabled, newAuditMonthlyReportCron);
        }

        public boolean passwordPolicyEnabled() {
            return passwordPolicyEnabled;
        }

        public boolean passwordHistoryEnabled() {
            return passwordHistoryEnabled;
        }

        public boolean accountLockEnabled() {
            return accountLockEnabled;
        }

        public List<String> enabledLoginTypes() {
            return enabledLoginTypes;
        }

        public long maxFileSizeBytes() {
            return maxFileSizeBytes;
        }

        public List<String> allowedFileExtensions() {
            return allowedFileExtensions;
        }

        public boolean strictMimeValidation() {
            return strictMimeValidation;
        }

        public int fileRetentionDays() {
            return fileRetentionDays;
        }

        public boolean auditEnabled() {
            return auditEnabled;
        }

        public boolean auditReasonRequired() {
            return auditReasonRequired;
        }

        public boolean auditSensitiveApiDefaultOn() {
            return auditSensitiveApiDefaultOn;
        }

        public int auditRetentionDays() {
            return auditRetentionDays;
        }

        public boolean auditStrictMode() {
            return auditStrictMode;
        }

        public String auditRiskLevel() {
            return auditRiskLevel;
        }

        public boolean auditMaskingEnabled() {
            return auditMaskingEnabled;
        }

        public List<String> auditSensitiveEndpoints() {
            return auditSensitiveEndpoints;
        }

        public List<String> auditUnmaskRoles() {
            return auditUnmaskRoles;
        }

        public boolean auditPartitionEnabled() {
            return auditPartitionEnabled;
        }

        public String auditPartitionCron() {
            return auditPartitionCron;
        }

        public int auditPartitionPreloadMonths() {
            return auditPartitionPreloadMonths;
        }

        public boolean auditMonthlyReportEnabled() {
            return auditMonthlyReportEnabled;
        }

        public String auditMonthlyReportCron() {
            return auditMonthlyReportCron;
        }
    }

    public static final class DatabasePolicySettingsProvider implements PolicySettingsProvider {

        private final PolicyAdminService service;

        public DatabasePolicySettingsProvider(PolicyAdminService service) {
            this.service = service;
        }

        @Override
        public PolicyToggleSettings currentSettings() {
            return service.currentState().toSettings();
        }
    }
}
