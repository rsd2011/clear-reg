package com.example.common.cache;

import org.springframework.data.domain.Pageable;

/**
 * Utility helpers for generating stable cache keys for organization-scoped lookups.
 */
public final class CacheKeyUtils {

    private CacheKeyUtils() {
    }

    /**
     * Builds a cache key that encodes pagination information for an organization-specific query.
     *
     * @param organizationCode organization identifier, defaults to {@code UNKNOWN}
     * @param pageable         pagination request, defaults to first page + unsorted
     * @return cache key string safe for cache name usage
     */
    public static String organizationScopeKey(String organizationCode, Pageable pageable) {
        String code = organizationCode == null ? "UNKNOWN" : organizationCode;
        String sort = pageable == null || pageable.getSort() == null
                ? "UNSORTED"
                : pageable.getSort().toString();
        if (pageable == null) {
            return code + "|0|0|" + sort;
        }
        return code + "|" + pageable.getPageNumber() + "|" + pageable.getPageSize() + "|" + sort;
    }
}
