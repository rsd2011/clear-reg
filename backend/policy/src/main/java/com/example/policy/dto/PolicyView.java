package com.example.policy.dto;

import java.util.List;

public record PolicyView(boolean passwordPolicyEnabled,
                         boolean passwordHistoryEnabled,
                         boolean accountLockEnabled,
                         List<String> enabledLoginTypes,
                         String yaml) {
}
