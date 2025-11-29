package com.example.common.policy;

import java.time.Instant;
import java.util.List;

/**
 * 마스킹 정책 조회 쿼리.
 */
public record MaskingQuery(String featureCode,
                           String actionCode,
                           String permGroupCode,
                           List<String> orgGroupCodes,
                           String dataKind,
                           Instant now) {
    public Instant nowOrDefault() {
        return now != null ? now : Instant.now();
    }
}
