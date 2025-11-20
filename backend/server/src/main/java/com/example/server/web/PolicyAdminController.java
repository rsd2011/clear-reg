package com.example.server.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.RequirePermission;
import com.example.policy.dto.PolicyUpdateRequest;
import com.example.policy.dto.PolicyView;
import com.example.policy.dto.PolicyYamlRequest;
import com.example.server.service.CacheMaintenanceService;
import com.example.server.policy.PolicyAdminPort;
import com.example.server.web.dto.CacheClearRequest;
import com.example.server.web.dto.CacheClearResponse;

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

    private final PolicyAdminPort policyAdminPort;
    private final CacheMaintenanceService cacheMaintenanceService;

    public PolicyAdminController(PolicyAdminPort policyAdminPort,
                                 CacheMaintenanceService cacheMaintenanceService) {
        this.policyAdminPort = policyAdminPort;
        this.cacheMaintenanceService = cacheMaintenanceService;
    }

    @GetMapping
    @Operation(summary = "Get current policy configuration")
    public ResponseEntity<PolicyView> currentPolicy() {
        return ResponseEntity.ok(policyAdminPort.currentPolicy());
    }

    @PutMapping("/toggles")
    @Operation(summary = "Update feature toggles for policies")
    @RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.UPDATE)
    public ResponseEntity<PolicyView> updateToggles(@Valid @RequestBody PolicyUpdateRequest request) {
        return ResponseEntity.ok(policyAdminPort.updateToggles(request));
    }

    @PutMapping("/yaml")
    @Operation(summary = "Update policy definition using YAML")
    @RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.UPDATE)
    public ResponseEntity<PolicyView> updateFromYaml(@Valid @RequestBody PolicyYamlRequest request) {
        return ResponseEntity.ok(policyAdminPort.updateFromYaml(request));
    }

    @PostMapping("/caches/clear")
    @Operation(summary = "Clear policy-related caches")
    @RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.UPDATE)
    public ResponseEntity<CacheClearResponse> clearCaches(@RequestBody(required = false) CacheClearRequest request) {
        CacheClearResponse response = new CacheClearResponse(
                cacheMaintenanceService.clearCaches(request == null ? null : request.caches()));
        return ResponseEntity.accepted().body(response);
    }
}
