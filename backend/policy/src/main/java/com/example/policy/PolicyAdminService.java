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
                snapshot.yaml());
    }

    public static final class PolicyState {

        private final boolean passwordPolicyEnabled;
        private final boolean passwordHistoryEnabled;
        private final boolean accountLockEnabled;
        private final List<String> enabledLoginTypes;

        private PolicyState(boolean passwordPolicyEnabled,
                            boolean passwordHistoryEnabled,
                            boolean accountLockEnabled,
                            List<String> enabledLoginTypes) {
            this.passwordPolicyEnabled = passwordPolicyEnabled;
            this.passwordHistoryEnabled = passwordHistoryEnabled;
            this.accountLockEnabled = accountLockEnabled;
            this.enabledLoginTypes = List.copyOf(enabledLoginTypes);
        }

        public static PolicyState from(PolicyToggleSettings settings) {
            return new PolicyState(settings.passwordPolicyEnabled(),
                    settings.passwordHistoryEnabled(),
                    settings.accountLockEnabled(),
                    settings.enabledLoginTypes());
        }

        public PolicyToggleSettings toSettings() {
            return new PolicyToggleSettings(passwordPolicyEnabled,
                    passwordHistoryEnabled,
                    accountLockEnabled,
                    enabledLoginTypes);
        }

        public PolicyState merge(PolicyUpdateRequest request) {
            boolean newPasswordPolicyEnabled = request.passwordPolicyEnabled() != null ? request.passwordPolicyEnabled() : passwordPolicyEnabled;
            boolean newPasswordHistoryEnabled = request.passwordHistoryEnabled() != null ? request.passwordHistoryEnabled() : passwordHistoryEnabled;
            boolean newAccountLockEnabled = request.accountLockEnabled() != null ? request.accountLockEnabled() : accountLockEnabled;
            List<String> newTypes = request.enabledLoginTypes() != null ? request.enabledLoginTypes() : enabledLoginTypes;
            return new PolicyState(newPasswordPolicyEnabled, newPasswordHistoryEnabled, newAccountLockEnabled, newTypes);
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
