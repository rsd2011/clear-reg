package com.example.common.policy;

import java.util.List;

public record PolicyToggleSettings(boolean passwordPolicyEnabled,
                                   boolean passwordHistoryEnabled,
                                   boolean accountLockEnabled,
                                   List<String> enabledLoginTypes) {

    public PolicyToggleSettings {
        enabledLoginTypes = enabledLoginTypes == null ? List.of() : List.copyOf(enabledLoginTypes);
    }
}
