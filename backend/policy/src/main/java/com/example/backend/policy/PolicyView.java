package com.example.backend.policy;

import java.util.List;

import com.example.auth.LoginType;

public record PolicyView(boolean passwordPolicyEnabled,
                         boolean passwordHistoryEnabled,
                         boolean accountLockEnabled,
                         List<LoginType> enabledLoginTypes,
                         String yaml) {
}
