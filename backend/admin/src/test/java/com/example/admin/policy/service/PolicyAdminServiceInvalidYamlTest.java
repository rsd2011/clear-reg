package com.example.admin.policy.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import com.example.admin.policy.repository.PolicyDocumentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.common.policy.PolicyToggleSettings;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

class PolicyAdminServiceInvalidYamlTest {

    PolicyDocumentRepository repository = Mockito.mock(PolicyDocumentRepository.class);
    ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    PolicyToggleSettings defaults = new PolicyToggleSettings(false, false, false,
            java.util.List.of(), 0, java.util.List.of(), true, 30);

    @Test
    @DisplayName("잘못된 YAML을 적용하면 IllegalArgumentException을 던진다")
    void applyYaml_invalid_throws() {
        given(repository.findByCode("security.policy")).willReturn(Optional.empty());
        PolicyAdminService service = new PolicyAdminService(repository, yamlMapper, defaults, null);

        assertThatThrownBy(() -> service.applyYaml("not: [valid"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
