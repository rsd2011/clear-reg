package com.example.backend.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.policy.PolicyAdminService;
import com.example.backend.policy.PolicyAdminService.PolicyUpdateRequest;
import com.example.backend.policy.PolicyView;
import com.example.backend.policy.PolicyYamlRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/policies")
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class PolicyAdminController {

    private final PolicyAdminService policyAdminService;

    public PolicyAdminController(PolicyAdminService policyAdminService) {
        this.policyAdminService = policyAdminService;
    }

    @GetMapping
    public ResponseEntity<PolicyView> currentPolicy() {
        return ResponseEntity.ok(policyAdminService.currentView());
    }

    @PutMapping("/toggles")
    public ResponseEntity<PolicyView> updateToggles(@Valid @RequestBody PolicyUpdateRequest request) {
        return ResponseEntity.ok(policyAdminService.updateView(request));
    }

    @PutMapping("/yaml")
    public ResponseEntity<PolicyView> updateFromYaml(@Valid @RequestBody PolicyYamlRequest request) {
        return ResponseEntity.ok(policyAdminService.applyYamlView(request.yaml()));
    }
}
