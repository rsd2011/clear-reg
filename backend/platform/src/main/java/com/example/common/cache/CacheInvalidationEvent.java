package com.example.common.cache;

import java.time.Instant;

/**
 * 캐시 무효화 이벤트 페이로드.
 */
public record CacheInvalidationEvent(
        CacheInvalidationType type,
        String tenantId,
        String scopeId,
        Long version,
        Instant issuedAt
) {
}
