package com.example.server.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.policy.PolicyAdminService;
import com.example.policy.dto.PolicyUpdateRequest;
import com.example.policy.dto.PolicyView;
import com.example.policy.dto.PolicyYamlRequest;

@DisplayName("PolicyAdminPortAdapter 테스트")
class PolicyAdminPortAdapterTest {

    private final PolicyAdminService policyAdminService = Mockito.mock(PolicyAdminService.class);
    private final PolicyAdminPortAdapter adapter = new PolicyAdminPortAdapter(policyAdminService);

    @Test
    @DisplayName("현재 정책 조회를 서비스에 위임한다")
    void currentPolicyDelegates() {
        PolicyView view = new PolicyView(true, true, true, List.of("PASSWORD"),
                1_048_576L, List.of("pdf"), true, 365,
                true, true, true, 730, true, "MEDIUM", true, List.of(), List.of(), "yaml");
        given(policyAdminService.currentView()).willReturn(view);

        PolicyView result = adapter.currentPolicy();

        assertThat(result).isEqualTo(view);
        then(policyAdminService).should().currentView();
    }

    @Test
    @DisplayName("토글 업데이트를 서비스에 위임한다")
    void updateTogglesDelegates() {
        PolicyUpdateRequest request = new PolicyUpdateRequest(false, null, null, List.of("SSO"),
                null, null, null, null, null, null, null, null, null, null, null, null, null);
        given(policyAdminService.updateView(request)).willReturn(new PolicyView(false, true, true,
                List.of("SSO"), 1_048_576L, List.of("pdf"), true, 365,
                true, true, true, 730, true, "MEDIUM", true, List.of(), List.of(), "yaml"));

        PolicyView updated = adapter.updateToggles(request);

        assertThat(updated.passwordPolicyEnabled()).isFalse();
        then(policyAdminService).should().updateView(request);
    }

    @Test
    @DisplayName("YAML 업데이트도 서비스에 위임한다")
    void updateFromYamlDelegates() {
        PolicyYamlRequest yamlRequest = new PolicyYamlRequest("policy: value");
        PolicyView view = new PolicyView(true, true, true, List.of("PASSWORD"),
                1_048_576L, List.of("pdf"), true, 365,
                true, true, true, 730, true, "MEDIUM", true, List.of(), List.of(), "policy: value");
        given(policyAdminService.applyYamlView(yamlRequest.yaml())).willReturn(view);

        PolicyView result = adapter.updateFromYaml(yamlRequest);

        assertThat(result.yaml()).isEqualTo("policy: value");
        then(policyAdminService).should().applyYamlView(yamlRequest.yaml());
    }
}
