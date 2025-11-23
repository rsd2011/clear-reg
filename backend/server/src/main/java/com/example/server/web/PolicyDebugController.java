package com.example.server.web;

import java.time.Instant;
import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.RequirePermission;
import com.example.common.policy.DataPolicyProvider;
import com.example.common.policy.DataPolicyQuery;
import com.example.common.policy.PolicySettingsProvider;
import com.example.server.policy.PolicyDebugResponse;

@RestController
@Validated
@RequestMapping("/api/admin/policies")
public class PolicyDebugController {

    private final PolicySettingsProvider policySettingsProvider;
    private final DataPolicyProvider dataPolicyProvider;

    public PolicyDebugController(PolicySettingsProvider policySettingsProvider,
                                 DataPolicyProvider dataPolicyProvider) {
        this.policySettingsProvider = policySettingsProvider;
        this.dataPolicyProvider = dataPolicyProvider;
    }

    @GetMapping("/effective")
    @RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.READ)
    public PolicyDebugResponse effective(@RequestParam String featureCode,
                                         @RequestParam(required = false) String actionCode,
                                         @RequestParam(required = false) String permGroupCode,
                                         @RequestParam(required = false) List<String> orgGroupCodes,
                                         @RequestParam(required = false) String businessType) {
        var toggles = policySettingsProvider.currentSettings();
        var query = new DataPolicyQuery(featureCode, actionCode, permGroupCode, null, orgGroupCodes, businessType, Instant.now());
        var match = dataPolicyProvider.evaluate(query).orElse(null);
        return PolicyDebugResponse.of(toggles, match);
    }
}
