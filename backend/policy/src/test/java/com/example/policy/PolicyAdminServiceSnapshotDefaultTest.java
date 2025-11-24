package com.example.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.common.policy.PolicyToggleSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

class PolicyAdminServiceSnapshotDefaultTest {

    PolicyDocumentRepository repository = Mockito.mock(PolicyDocumentRepository.class);
    ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    PolicyToggleSettings defaults = new PolicyToggleSettings(false, false, false,
            java.util.List.of(), 0, java.util.List.of(), true, 30);

    @Test
    @DisplayName("기본 설정만 있을 때 snapshot은 empty YAML이 아닌 기본값 YAML을 반환한다")
    void snapshot_returnsYamlFromDefaults() {
        given(repository.findByCode("security.policy")).willReturn(Optional.empty());
        PolicyAdminService service = new PolicyAdminService(repository, yamlMapper, defaults, null);

        PolicyAdminService.PolicySnapshot snapshot = service.snapshot();

        assertThat(snapshot.yaml()).isNotBlank();
    }
}
