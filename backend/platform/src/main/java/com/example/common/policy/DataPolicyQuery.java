package com.example.common.policy;

import java.time.Instant;

public record DataPolicyQuery(String featureCode,
                              String actionCode,
                              String permGroupCode,
                              Long orgPolicyId,
                              java.util.List<String> orgGroupCodes,
                              String businessType,
                              String sensitiveTag,
                              Instant now) {
    public Instant nowOrDefault() {
        return now != null ? now : Instant.now();
    }
}
