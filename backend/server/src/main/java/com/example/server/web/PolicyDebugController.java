package com.example.server.web;

import java.time.Instant;
import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.admin.permission.annotation.RequirePermission;
import com.example.common.policy.MaskingPolicyProvider;
import com.example.common.policy.MaskingQuery;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.RowAccessPolicyProvider;
import com.example.common.policy.RowAccessQuery;
import com.example.server.policy.PolicyDebugResponse;

@RestController
@Validated
@RequestMapping("/api/admin/policies")
@Tag(name = "Policy Debug", description = "정책 매칭/토글 조회 디버그 API")
public class PolicyDebugController {

    private final PolicySettingsProvider policySettingsProvider;
    private final RowAccessPolicyProvider rowAccessPolicyProvider;
    private final MaskingPolicyProvider maskingPolicyProvider;

    public PolicyDebugController(PolicySettingsProvider policySettingsProvider,
                                 RowAccessPolicyProvider rowAccessPolicyProvider,
                                 MaskingPolicyProvider maskingPolicyProvider) {
        this.policySettingsProvider = policySettingsProvider;
        this.rowAccessPolicyProvider = rowAccessPolicyProvider;
        this.maskingPolicyProvider = maskingPolicyProvider;
    }

    @GetMapping("/effective")
    @RequirePermission(feature = FeatureCode.POLICY, action = ActionCode.READ)
    public PolicyDebugResponse effective(@RequestParam String featureCode,
                                         @RequestParam(required = false) String actionCode,
                                         @RequestParam(required = false) String permGroupCode,
                                         @RequestParam(required = false) List<String> orgGroupCodes,
                                         @RequestParam(required = false) String dataKind) {
        var toggles = policySettingsProvider.currentSettings();
        Instant now = Instant.now();

        var rowAccessQuery = new RowAccessQuery(featureCode, actionCode, permGroupCode, orgGroupCodes, now);
        var rowAccessMatch = rowAccessPolicyProvider.evaluate(rowAccessQuery).orElse(null);

        var maskingQuery = new MaskingQuery(featureCode, actionCode, permGroupCode, orgGroupCodes, dataKind, now);
        var maskingMatch = maskingPolicyProvider.evaluate(maskingQuery).orElse(null);

        return PolicyDebugResponse.of(toggles, rowAccessMatch, maskingMatch);
    }
}
