package com.example.server.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.admin.permission.annotation.RequirePermission;
import com.example.dw.application.dto.DwIngestionPolicyUpdateRequest;
import com.example.dw.application.policy.DwIngestionPolicyView;
import com.example.dw.application.port.DwIngestionPolicyPort;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/dw-ingestion/policy")
@Tag(name = "DW Ingestion Policy", description = "DW 수집 정책 관리 API")
@Validated
@PreAuthorize("hasRole('ADMIN')")
@RequirePermission(feature = FeatureCode.HR_IMPORT, action = ActionCode.READ)
public class DwIngestionPolicyController {

    private final DwIngestionPolicyPort policyPort;

    public DwIngestionPolicyController(DwIngestionPolicyPort policyPort) {
        this.policyPort = policyPort;
    }

    @GetMapping
    @Operation(summary = "Get current DW ingestion policy")
    public DwIngestionPolicyView currentPolicy() {
        return policyPort.currentPolicy();
    }

    @PutMapping
    @Operation(summary = "Update DW ingestion policy")
    @RequirePermission(feature = FeatureCode.HR_IMPORT, action = ActionCode.UPDATE)
    public DwIngestionPolicyView update(@Valid @RequestBody DwIngestionPolicyUpdateRequest request) {
        return policyPort.updatePolicy(request);
    }
}
