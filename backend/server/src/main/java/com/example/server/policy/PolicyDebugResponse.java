package com.example.server.policy;

import com.example.common.policy.DataPolicyMatch;
import com.example.common.policy.PolicyToggleSettings;

public record PolicyDebugResponse(PolicyToggleSettings policyToggles,
                                  DataPolicyMatch dataPolicyMatch) {
    public static PolicyDebugResponse of(PolicyToggleSettings toggles, DataPolicyMatch match) {
        return new PolicyDebugResponse(toggles, match);
    }
}
