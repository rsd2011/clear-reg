package com.example.policy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.common.policy.PolicyToggleSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

class PolicyAdminServiceApplyYamlViewInvalidTest {

    PolicyDocumentRepository repository = Mockito.mock(PolicyDocumentRepository.class);
    ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    PolicyToggleSettings defaults = new PolicyToggleSettings(false, false, false,
            java.util.List.of(), 0, java.util.List.of(), true, 30);

    @Test
    @DisplayName("applyYamlView가 잘못된 YAML을 받으면 IllegalArgumentException을 던진다")
    void applyYamlView_invalid_throws() {
        given(repository.findByCode("security.policy")).willReturn(Optional.empty());
        PolicyAdminService service = new PolicyAdminService(repository, yamlMapper, defaults, null);

        assertThatThrownBy(() -> service.applyYamlView("bad: [yaml"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
