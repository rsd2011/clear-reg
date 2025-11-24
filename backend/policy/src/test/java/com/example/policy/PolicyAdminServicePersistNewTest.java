package com.example.policy;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.example.common.policy.PolicyToggleSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

class PolicyAdminServicePersistNewTest {

    PolicyDocumentRepository repository = Mockito.mock(PolicyDocumentRepository.class);
    ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    PolicyToggleSettings defaults = new PolicyToggleSettings(false, false, false,
            java.util.List.of(), 0, java.util.List.of(), true, 30);

    @Test
    @DisplayName("기존 문서가 없을 때 applyYamlView는 새 문서를 저장한다")
    void applyYamlView_savesNewDocument() {
        given(repository.findByCode("security.policy")).willReturn(Optional.empty());
        PolicyAdminService service = new PolicyAdminService(repository, yamlMapper, defaults, null);

        service.applyYamlView("""
                passwordPolicyEnabled: true
                passwordHistoryEnabled: false
                accountLockEnabled: false
                enabledLoginTypes: []
                maxFileSizeBytes: 1024
                allowedFileExtensions: []
                strictMimeValidation: true
                fileRetentionDays: 30
                """);

        verify(repository).save(ArgumentMatchers.any(PolicyDocument.class));
    }
}
