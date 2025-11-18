package com.example.server.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.RequirePermission;
import com.example.hr.application.policy.HrIngestionPolicyService;
import com.example.hr.application.policy.HrIngestionPolicyUpdateRequest;
import com.example.hr.application.policy.HrIngestionPolicyView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/hr-ingestion/policy")
@Tag(name = "HR Ingestion Policy")
@Validated
@PreAuthorize("hasRole('ADMIN')")
@RequirePermission(feature = FeatureCode.HR_IMPORT, action = ActionCode.READ)
public class HrIngestionPolicyController {

    private final HrIngestionPolicyService policyService;

    public HrIngestionPolicyController(HrIngestionPolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping
    @Operation(summary = "Get current HR ingestion policy")
    public HrIngestionPolicyView currentPolicy() {
        return policyService.view();
    }

    @PutMapping
    @Operation(summary = "Update HR ingestion policy")
    @RequirePermission(feature = FeatureCode.HR_IMPORT, action = ActionCode.UPDATE)
    public HrIngestionPolicyView update(@Valid @RequestBody HrIngestionPolicyUpdateRequest request) {
        return policyService.update(request);
    }
}
