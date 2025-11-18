package com.example.policy;

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

import com.example.common.policy.PolicyToggleSettings;
import com.example.policy.dto.PolicyUpdateRequest;
import com.example.policy.dto.PolicyView;
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
        PolicyToggleSettings defaults = new PolicyToggleSettings(true, true, true, List.of("PASSWORD"));
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        given(repository.findByCode("security.policy")).willReturn(Optional.empty());
        service = new PolicyAdminService(repository, mapper, defaults);
    }

    @Test
    @DisplayName("Given update request When update Then persist merged state")
    void givenRequestWhenUpdateThenPersist() {
        PolicyUpdateRequest request = new PolicyUpdateRequest(
                false, null, true, List.of("SSO"));

        PolicyAdminService.PolicySnapshot snapshot = service.update(request);

        assertThat(snapshot.state().passwordPolicyEnabled()).isFalse();
        assertThat(snapshot.state().enabledLoginTypes()).containsExactly("SSO");
        ArgumentCaptor<PolicyDocument> captor = ArgumentCaptor.forClass(PolicyDocument.class);
        then(repository).should().save(captor.capture());
        assertThat(captor.getValue().getYaml()).contains("enabledLoginTypes");
    }

    @Test
    @DisplayName("Given YAML When applyYaml Then refresh cache")
    void givenYamlWhenApplyThenRefresh() {
        String yaml = "passwordPolicyEnabled: false\naccountLockEnabled: false\npasswordHistoryEnabled: false\nenabledLoginTypes:\n  - SSO\n";

        PolicyAdminService.PolicySnapshot snapshot = service.applyYaml(yaml);

        assertThat(snapshot.state().enabledLoginTypes()).containsExactly("SSO");
        then(repository).should().save(any());
    }

    @Test
    @DisplayName("Given invalid YAML When applyYaml Then throw")
    void givenInvalidYamlWhenApplyThenThrow() {
        assertThatThrownBy(() -> service.applyYaml("invalid: ["))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Given defaults When currentView Then expose cached state")
    void givenDefaultsWhenCurrentViewThenExpose() {
        PolicyView view = service.currentView();
        assertThat(view.passwordPolicyEnabled()).isTrue();
        assertThat(view.enabledLoginTypes()).contains("PASSWORD");
    }

    @Test
    @DisplayName("Given update request When updateView Then include rendered yaml")
    void givenUpdateRequestWhenUpdateViewThenIncludeYaml() {
        PolicyUpdateRequest request = new PolicyUpdateRequest(true, true, false,
                List.of("AD"));

        PolicyView view = service.updateView(request);

        assertThat(view.yaml()).contains("AD");
        assertThat(view.accountLockEnabled()).isFalse();
    }

    @Test
    @DisplayName("Given YAML When applyYamlView Then return PolicyView")
    void givenYamlWhenApplyYamlViewThenReturnView() {
        String yaml = "passwordPolicyEnabled: true\npasswordHistoryEnabled: true\naccountLockEnabled: true\nenabledLoginTypes:\n  - PASSWORD\n";
        PolicyView view = service.applyYamlView(yaml);
        assertThat(view.passwordHistoryEnabled()).isTrue();
    }
}
