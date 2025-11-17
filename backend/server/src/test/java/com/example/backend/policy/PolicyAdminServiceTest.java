package com.example.backend.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.auth.LoginType;
import com.example.auth.config.PolicyToggleProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@DisplayName("PolicyAdminService")
class PolicyAdminServiceTest {

    @Mock
    private PolicyDocumentRepository repository;

    private PolicyAdminService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        PolicyToggleProperties defaults = new PolicyToggleProperties();
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        given(repository.findByCode("security.policy")).willReturn(Optional.empty());
        service = new PolicyAdminService(repository, mapper, defaults);
    }

    @Test
    @DisplayName("Given update request When update Then persist merged state")
    void givenRequestWhenUpdateThenPersist() {
        PolicyAdminService.PolicyUpdateRequest request = new PolicyAdminService.PolicyUpdateRequest(
                false, null, true, List.of(LoginType.SSO));

        PolicyAdminService.PolicySnapshot snapshot = service.update(request);

        assertThat(snapshot.state().passwordPolicyEnabled()).isFalse();
        assertThat(snapshot.state().enabledLoginTypes()).containsExactly(LoginType.SSO);
        ArgumentCaptor<PolicyDocument> captor = ArgumentCaptor.forClass(PolicyDocument.class);
        then(repository).should().save(captor.capture());
        assertThat(captor.getValue().getYaml()).contains("enabledLoginTypes");
    }

    @Test
    @DisplayName("Given YAML When applyYaml Then refresh cache")
    void givenYamlWhenApplyThenRefresh() {
        String yaml = "passwordPolicyEnabled: false\naccountLockEnabled: false\npasswordHistoryEnabled: false\nenabledLoginTypes:\n  - SSO\n";

        PolicyAdminService.PolicySnapshot snapshot = service.applyYaml(yaml);

        assertThat(snapshot.state().enabledLoginTypes()).containsExactly(LoginType.SSO);
        then(repository).should().save(any());
    }

    @Test
    @DisplayName("Given invalid YAML When applyYaml Then throw")
    void givenInvalidYamlWhenApplyThenThrow() {
        assertThatThrownBy(() -> service.applyYaml("invalid: ["))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
