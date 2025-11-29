package com.example.common.policy;

import java.time.Instant;
import java.util.List;

/**
 * 행 수준 접근 정책 조회 쿼리.
 */
public record RowAccessQuery(String featureCode,
                             String actionCode,
                             String permGroupCode,
                             List<String> orgGroupCodes,
                             Instant now) {
    public Instant nowOrDefault() {
        return now != null ? now : Instant.now();
    }
}
