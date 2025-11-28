package com.example.admin.policy.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.example.admin.policy.domain.PolicyDocument;
import com.example.admin.policy.repository.PolicyDocumentRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.common.policy.AuditPartitionSettings;
import com.example.common.policy.PolicyChangedEvent;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;
import com.example.admin.policy.dto.PolicyUpdateRequest;
import com.example.admin.policy.dto.PolicyView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.example.common.schedule.BatchJobCode;
import com.example.common.schedule.BatchJobSchedule;

@Service
public class PolicyAdminService {

    private static final String DOCUMENT_CODE = "security.policy";

    private final PolicyDocumentRepository repository;
    private final ObjectMapper yamlMapper;
    private final AtomicReference<PolicyState> cache;
    private final ApplicationEventPublisher eventPublisher;

    public PolicyAdminService(PolicyDocumentRepository repository,
                              @Qualifier("yamlObjectMapper") ObjectMapper yamlMapper,
                              PolicyToggleSettings defaults,
                              ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.yamlMapper = yamlMapper.copy();
        PolicyState initial = PolicyState.from(defaults, null, com.example.common.schedule.BatchJobDefaults.defaults());
        this.cache = new AtomicReference<>(initial);
        this.eventPublisher = eventPublisher;
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
        publishChange(exportYaml(updated));
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
        publishChange(yaml);
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

    private void publishChange(String yaml) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new PolicyChangedEvent(DOCUMENT_CODE, yaml));
        }
    }

    private PolicyState readState(String yaml) {
        try {
            JsonNode root = yamlMapper.readTree(yaml);
            AuditPartitionSettings partition = null;
            JsonNode partitionNode = root.get("auditPartition");
            if (partitionNode != null) {
                partition = yamlMapper.treeToValue(partitionNode, AuditPartitionSettings.class);
            }
            Map<BatchJobCode, BatchJobSchedule> batchSchedules = parseBatchJobs(root);
            ObjectNode toggleNode = root.isObject() ? ((ObjectNode) root).deepCopy() : yamlMapper.createObjectNode();
            toggleNode.remove("auditPartition");
            toggleNode.remove("auditPartitionTablespaceHot");
            toggleNode.remove("auditPartitionTablespaceCold");
            toggleNode.remove("auditPartitionHotMonths");
            toggleNode.remove("auditPartitionColdMonths");
            toggleNode.remove("batchJobs");
            PolicyToggleSettings settings = yamlMapper.treeToValue(toggleNode, PolicyToggleSettings.class);
            if (partition == null && (root.has("auditPartitionTablespaceHot") || root.has("auditPartitionTablespaceCold")
                    || root.has("auditPartitionHotMonths") || root.has("auditPartitionColdMonths"))) {
                partition = new AuditPartitionSettings(settings.auditPartitionEnabled(), settings.auditPartitionCron(),
                        settings.auditPartitionPreloadMonths(),
                        root.path("auditPartitionTablespaceHot").asText(""),
                        root.path("auditPartitionTablespaceCold").asText(""),
                        root.path("auditPartitionHotMonths").asInt(6),
                        root.path("auditPartitionColdMonths").asInt(60));
            }
            return PolicyState.from(settings, partition, batchSchedules);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid policy YAML", exception);
        }
    }

    private String exportYaml(PolicyState state) {
        try {
            ObjectNode root = yamlMapper.valueToTree(state.toSettings());
            root.set("auditPartition", yamlMapper.valueToTree(state.toPartitionSettings()));
            if (!state.batchJobs().isEmpty()) {
                ObjectNode batchNode = yamlMapper.createObjectNode();
                state.batchJobs().forEach((code, sched) -> batchNode.set(code.name(), yamlMapper.valueToTree(sched)));
                root.set("batchJobs", batchNode);
            }
            return yamlMapper.writeValueAsString(root);
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
                state.auditPartitionTablespaceHot(),
                state.auditPartitionTablespaceCold(),
                state.auditPartitionHotMonths(),
                state.auditPartitionColdMonths(),
                state.auditMonthlyReportEnabled(),
                state.auditMonthlyReportCron(),
                state.auditLogRetentionEnabled(),
                state.auditLogRetentionCron(),
                state.auditColdArchiveEnabled(),
                state.auditColdArchiveCron(),
                state.auditRetentionCleanupEnabled(),
                state.auditRetentionCleanupCron(),
                state.batchJobs(),
                snapshot.yaml());
    }

    private Map<BatchJobCode, BatchJobSchedule> parseBatchJobs(JsonNode root) throws JsonProcessingException {
        JsonNode batchNode = root.get("batchJobs");
        if (batchNode == null || !batchNode.isObject()) {
            return Map.of();
        }
        Map<BatchJobCode, BatchJobSchedule> result = new java.util.HashMap<>();
        var fields = batchNode.fields();
        while (fields.hasNext()) {
            var entry = fields.next();
            try {
                BatchJobCode code = BatchJobCode.valueOf(entry.getKey());
                BatchJobSchedule schedule = yamlMapper.treeToValue(entry.getValue(), BatchJobSchedule.class);
                result.put(code, schedule);
            } catch (IllegalArgumentException ignore) {
                // unknown code -> skip
            }
        }
        return result;
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
        private final String auditPartitionTablespaceHot;
        private final String auditPartitionTablespaceCold;
        private final int auditPartitionHotMonths;
        private final int auditPartitionColdMonths;
        private final boolean auditMonthlyReportEnabled;
        private final String auditMonthlyReportCron;
        private final boolean auditLogRetentionEnabled;
        private final String auditLogRetentionCron;
        private final boolean auditColdArchiveEnabled;
        private final String auditColdArchiveCron;
        private final boolean auditRetentionCleanupEnabled;
        private final String auditRetentionCleanupCron;
        private final Map<BatchJobCode, BatchJobSchedule> batchJobs;

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
                            Map<BatchJobCode, BatchJobSchedule> batchJobs) {
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
            this.auditPartitionTablespaceHot = auditPartitionTablespaceHot == null ? "" : auditPartitionTablespaceHot;
            this.auditPartitionTablespaceCold = auditPartitionTablespaceCold == null ? "" : auditPartitionTablespaceCold;
            this.auditPartitionHotMonths = auditPartitionHotMonths <= 0 ? 6 : auditPartitionHotMonths;
            this.auditPartitionColdMonths = auditPartitionColdMonths <= 0 ? 60 : auditPartitionColdMonths;
            this.auditMonthlyReportEnabled = auditMonthlyReportEnabled;
            this.auditMonthlyReportCron = auditMonthlyReportCron;
            this.auditLogRetentionEnabled = auditLogRetentionEnabled;
            this.auditLogRetentionCron = auditLogRetentionCron;
            this.auditColdArchiveEnabled = auditColdArchiveEnabled;
            this.auditColdArchiveCron = auditColdArchiveCron;
            this.auditRetentionCleanupEnabled = auditRetentionCleanupEnabled;
            this.auditRetentionCleanupCron = auditRetentionCleanupCron;
            this.batchJobs = batchJobs == null ? Map.of() : Map.copyOf(batchJobs);
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
                    "",
                    "",
                    6,
                    60,
                    settings.auditMonthlyReportEnabled(),
                    settings.auditMonthlyReportCron(),
                    settings.auditLogRetentionEnabled(),
                    settings.auditLogRetentionCron(),
                    settings.auditColdArchiveEnabled(),
                    settings.auditColdArchiveCron(),
                    settings.auditRetentionCleanupEnabled(),
                    settings.auditRetentionCleanupCron(),
                    settings.batchJobs());
        }

        public static PolicyState from(PolicyToggleSettings settings, AuditPartitionSettings partition, Map<BatchJobCode, BatchJobSchedule> batchJobs) {
            AuditPartitionSettings ps = partition == null
                    ? new AuditPartitionSettings(settings.auditPartitionEnabled(), settings.auditPartitionCron(),
                    settings.auditPartitionPreloadMonths(), "", "", 6, 60)
                    : partition;
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
                    ps.enabled(),
                    ps.cron(),
                    ps.preloadMonths(),
                    ps.tablespaceHot(),
                    ps.tablespaceCold(),
                    ps.hotMonths(),
                    ps.coldMonths(),
                    settings.auditMonthlyReportEnabled(),
                    settings.auditMonthlyReportCron(),
                    settings.auditLogRetentionEnabled(),
                    settings.auditLogRetentionCron(),
                    settings.auditColdArchiveEnabled(),
                    settings.auditColdArchiveCron(),
                    settings.auditRetentionCleanupEnabled(),
                    settings.auditRetentionCleanupCron(),
                    batchJobs == null ? Map.of() : batchJobs);
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
                    auditUnmaskRoles,
                    auditPartitionEnabled,
                    auditPartitionCron,
                    auditPartitionPreloadMonths,
                    auditMonthlyReportEnabled,
                    auditMonthlyReportCron,
                    auditLogRetentionEnabled,
                    auditLogRetentionCron,
                    auditColdArchiveEnabled,
                    auditColdArchiveCron,
                    auditRetentionCleanupEnabled,
                    auditRetentionCleanupCron,
                    batchJobs);
        }

        public AuditPartitionSettings toPartitionSettings() {
            return new AuditPartitionSettings(auditPartitionEnabled, auditPartitionCron, auditPartitionPreloadMonths,
                    auditPartitionTablespaceHot, auditPartitionTablespaceCold, auditPartitionHotMonths, auditPartitionColdMonths);
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
            String newTablespaceHot = request.auditPartitionTablespaceHot() != null ? request.auditPartitionTablespaceHot() : auditPartitionTablespaceHot;
            String newTablespaceCold = request.auditPartitionTablespaceCold() != null ? request.auditPartitionTablespaceCold() : auditPartitionTablespaceCold;
            int newHotMonths = request.auditPartitionHotMonths() != null ? request.auditPartitionHotMonths() : auditPartitionHotMonths;
            int newColdMonths = request.auditPartitionColdMonths() != null ? request.auditPartitionColdMonths() : auditPartitionColdMonths;
            boolean newAuditMonthlyReportEnabled = request.auditMonthlyReportEnabled() != null ? request.auditMonthlyReportEnabled() : auditMonthlyReportEnabled;
            String newAuditMonthlyReportCron = request.auditMonthlyReportCron() != null ? request.auditMonthlyReportCron() : auditMonthlyReportCron;
            boolean newAuditLogRetentionEnabled = request.auditLogRetentionEnabled() != null ? request.auditLogRetentionEnabled() : auditLogRetentionEnabled;
            String newAuditLogRetentionCron = request.auditLogRetentionCron() != null ? request.auditLogRetentionCron() : auditLogRetentionCron;
            boolean newAuditColdArchiveEnabled = request.auditColdArchiveEnabled() != null ? request.auditColdArchiveEnabled() : auditColdArchiveEnabled;
            String newAuditColdArchiveCron = request.auditColdArchiveCron() != null ? request.auditColdArchiveCron() : auditColdArchiveCron;
            boolean newAuditRetentionCleanupEnabled = request.auditRetentionCleanupEnabled() != null ? request.auditRetentionCleanupEnabled() : auditRetentionCleanupEnabled;
            String newAuditRetentionCleanupCron = request.auditRetentionCleanupCron() != null ? request.auditRetentionCleanupCron() : auditRetentionCleanupCron;
            Map<BatchJobCode, BatchJobSchedule> newBatchJobs = request.batchJobs() != null ? request.batchJobs() : batchJobs;

            return new PolicyState(newPasswordPolicyEnabled, newPasswordHistoryEnabled, newAccountLockEnabled, newTypes,
                    newMaxFileSize, newExtensions, newStrictMime, Math.max(newRetention, 0),
                    newAuditEnabled, newAuditReasonRequired, newAuditSensitiveApiDefaultOn,
                    Math.max(newAuditRetention, 0), newAuditStrictMode, newAuditRiskLevel, newAuditMaskingEnabled, newAuditSensitiveEndpoints, newAuditUnmaskRoles,
                    newAuditPartitionEnabled, newAuditPartitionCron, Math.max(newAuditPartitionPreloadMonths, 0),
                    newTablespaceHot, newTablespaceCold, Math.max(newHotMonths, 1), Math.max(newColdMonths, 1),
                    newAuditMonthlyReportEnabled, newAuditMonthlyReportCron,
                    newAuditLogRetentionEnabled, newAuditLogRetentionCron,
                    newAuditColdArchiveEnabled, newAuditColdArchiveCron,
                    newAuditRetentionCleanupEnabled, newAuditRetentionCleanupCron,
                    newBatchJobs == null ? Map.of() : newBatchJobs);
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

        public String auditPartitionTablespaceHot() {
            return auditPartitionTablespaceHot;
        }

        public String auditPartitionTablespaceCold() {
            return auditPartitionTablespaceCold;
        }

        public int auditPartitionHotMonths() {
            return auditPartitionHotMonths;
        }

        public int auditPartitionColdMonths() {
            return auditPartitionColdMonths;
        }

        public boolean auditMonthlyReportEnabled() {
            return auditMonthlyReportEnabled;
        }

        public String auditMonthlyReportCron() {
            return auditMonthlyReportCron;
        }

        public boolean auditLogRetentionEnabled() {
            return auditLogRetentionEnabled;
        }

        public String auditLogRetentionCron() {
            return auditLogRetentionCron;
        }

        public boolean auditColdArchiveEnabled() {
            return auditColdArchiveEnabled;
        }

        public String auditColdArchiveCron() {
            return auditColdArchiveCron;
        }

        public boolean auditRetentionCleanupEnabled() {
            return auditRetentionCleanupEnabled;
        }

        public String auditRetentionCleanupCron() {
            return auditRetentionCleanupCron;
        }

        public Map<BatchJobCode, BatchJobSchedule> batchJobs() {
            return batchJobs;
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

        @Override
        public AuditPartitionSettings partitionSettings() {
            return service.currentState().toPartitionSettings();
        }

        @Override
        public BatchJobSchedule batchJobSchedule(BatchJobCode code) {
            return service.currentState().batchJobs().get(code);
        }
    }
}
