package com.example.backend.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.hr.application.policy.HrIngestionPolicyService;
import com.example.hr.application.policy.HrIngestionPolicyUpdateRequest;
import com.example.hr.application.policy.HrIngestionPolicyView;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/hr-ingestion/policy")
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class HrIngestionPolicyController {

    private final HrIngestionPolicyService policyService;

    public HrIngestionPolicyController(HrIngestionPolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping
    public HrIngestionPolicyView currentPolicy() {
        return policyService.view();
    }

    @PutMapping
    public HrIngestionPolicyView update(@Valid @RequestBody HrIngestionPolicyUpdateRequest request) {
        return policyService.update(request);
    }
}
