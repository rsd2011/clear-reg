package com.example.server.web;

import org.springframework.http.ResponseEntity;
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
import com.example.policy.PolicyAdminService;
import com.example.policy.dto.PolicyUpdateRequest;
import com.example.policy.dto.PolicyView;
import com.example.policy.dto.PolicyYamlRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/policies")
@Tag(name = "Policy Administration")
@Validated
@PreAuthorize("hasRole('ADMIN')")
@RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.READ)
public class PolicyAdminController {

    private final PolicyAdminService policyAdminService;

    public PolicyAdminController(PolicyAdminService policyAdminService) {
        this.policyAdminService = policyAdminService;
    }

    @GetMapping
    @Operation(summary = "Get current policy configuration")
    public ResponseEntity<PolicyView> currentPolicy() {
        return ResponseEntity.ok(policyAdminService.currentView());
    }

    @PutMapping("/toggles")
    @Operation(summary = "Update feature toggles for policies")
    @RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.UPDATE)
    public ResponseEntity<PolicyView> updateToggles(@Valid @RequestBody PolicyUpdateRequest request) {
        return ResponseEntity.ok(policyAdminService.updateView(request));
    }

    @PutMapping("/yaml")
    @Operation(summary = "Update policy definition using YAML")
    @RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.UPDATE)
    public ResponseEntity<PolicyView> updateFromYaml(@Valid @RequestBody PolicyYamlRequest request) {
        return ResponseEntity.ok(policyAdminService.applyYamlView(request.yaml()));
    }
}
