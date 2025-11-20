package com.example.server.policy;

import org.springframework.stereotype.Component;

import com.example.policy.PolicyAdminService;
import com.example.policy.dto.PolicyUpdateRequest;
import com.example.policy.dto.PolicyView;
import com.example.policy.dto.PolicyYamlRequest;

@Component
public class PolicyAdminPortAdapter implements PolicyAdminPort {

    private final PolicyAdminService policyAdminService;

    public PolicyAdminPortAdapter(PolicyAdminService policyAdminService) {
        this.policyAdminService = policyAdminService;
    }

    @Override
    public PolicyView currentPolicy() {
        return policyAdminService.currentView();
    }

    @Override
    public PolicyView updateToggles(PolicyUpdateRequest request) {
        return policyAdminService.updateView(request);
    }

    @Override
    public PolicyView updateFromYaml(PolicyYamlRequest request) {
        return policyAdminService.applyYamlView(request.yaml());
    }
}
