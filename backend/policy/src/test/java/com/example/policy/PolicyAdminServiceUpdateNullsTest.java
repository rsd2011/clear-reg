package com.example.policy;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.common.policy.PolicyToggleSettings;
import com.example.policy.dto.PolicyUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

class PolicyAdminServiceUpdateNullsTest {

    PolicyDocumentRepository repository = Mockito.mock(PolicyDocumentRepository.class);
    ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    PolicyToggleSettings defaults = new PolicyToggleSettings(false, false, false,
            java.util.List.of(), 1024, java.util.List.of(), true, 30);

    @Test
    @DisplayName("update 요청에서 null 필드가 들어오면 기존 상태를 유지한다")
    void update_keepsExistingWhenNull() {
        PolicyDocument doc = new PolicyDocument("security.policy", """
                passwordPolicyEnabled: true
                passwordHistoryEnabled: true
                accountLockEnabled: true
                enabledLoginTypes: [BASIC]
                maxFileSizeBytes: 2048
                allowedFileExtensions: [pdf]
                strictMimeValidation: true
                fileRetentionDays: 90
                """);
        given(repository.findByCode("security.policy")).willReturn(Optional.of(doc));
        PolicyAdminService service = new PolicyAdminService(repository, yamlMapper, defaults);

        PolicyUpdateRequest req = new PolicyUpdateRequest(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        service.update(req);

        verify(repository).save(Mockito.argThat(saved ->
                saved.getYaml().contains("passwordPolicyEnabled: true") &&
                        saved.getYaml().contains("maxFileSizeBytes: 2048")));
    }
}
