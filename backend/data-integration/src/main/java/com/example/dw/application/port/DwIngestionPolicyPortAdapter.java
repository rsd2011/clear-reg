package com.example.dw.application.port;

import org.springframework.stereotype.Component;

import com.example.dw.application.dto.DwIngestionPolicyUpdateRequest;
import com.example.dw.application.policy.DwIngestionPolicyService;
import com.example.dw.application.policy.DwIngestionPolicyView;

import lombok.RequiredArgsConstructor;

/**
 * Adapter implementing DwIngestionPolicyPort using DwIngestionPolicyService.
 */
@Component
@RequiredArgsConstructor
public class DwIngestionPolicyPortAdapter implements DwIngestionPolicyPort {

    private final DwIngestionPolicyService policyService;

    @Override
    public DwIngestionPolicyView currentPolicy() {
        return policyService.view();
    }

    @Override
    public DwIngestionPolicyView updatePolicy(DwIngestionPolicyUpdateRequest request) {
        return policyService.update(request);
    }
}
