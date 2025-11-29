package com.example.common.policy;

import java.time.Instant;

/**
 * @deprecated RowAccessQuery와 MaskingQuery를 사용하세요.
 */
@Deprecated(forRemoval = true)
public record DataPolicyQuery(String featureCode,
                              String actionCode,
                              String permGroupCode,
                              java.util.List<String> orgGroupCodes,
                              Instant now) {
    public Instant nowOrDefault() {
        return now != null ? now : Instant.now();
    }
}
