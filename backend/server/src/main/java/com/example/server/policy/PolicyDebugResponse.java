package com.example.server.policy;

import com.example.common.policy.MaskingMatch;
import com.example.common.policy.PolicyToggleSettings;
import com.example.common.policy.RowAccessMatch;

public record PolicyDebugResponse(PolicyToggleSettings policyToggles,
                                  RowAccessMatch rowAccessMatch,
                                  MaskingMatch maskingMatch) {
    public static PolicyDebugResponse of(PolicyToggleSettings toggles,
                                         RowAccessMatch rowAccess,
                                         MaskingMatch masking) {
        return new PolicyDebugResponse(toggles, rowAccess, masking);
    }
}
