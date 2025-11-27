package com.example.dwgateway.dw;

import org.springframework.stereotype.Component;

import com.example.dw.application.policy.DwIngestionPolicyService;
import com.example.dw.application.dto.DwIngestionPolicyUpdateRequest;
import com.example.dw.application.policy.DwIngestionPolicyView;

@Component
public class DwIngestionPolicyPortAdapter implements DwIngestionPolicyPort {

    private final DwIngestionPolicyService policyService;

    public DwIngestionPolicyPortAdapter(DwIngestionPolicyService policyService) {
        this.policyService = policyService;
    }

    @Override
    public DwIngestionPolicyView currentPolicy() {
        return policyService.view();
    }

    @Override
    public DwIngestionPolicyView updatePolicy(DwIngestionPolicyUpdateRequest request) {
        return policyService.update(request);
    }
}
