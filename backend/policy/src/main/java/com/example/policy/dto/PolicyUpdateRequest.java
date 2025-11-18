package com.example.policy.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record PolicyUpdateRequest(
        Boolean passwordPolicyEnabled,
        Boolean passwordHistoryEnabled,
        Boolean accountLockEnabled,
        List<@NotNull String> enabledLoginTypes) {

}
