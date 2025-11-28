package com.example.common.security;

import com.example.common.codegroup.annotation.ManagedCode;

/**
 * 행 단위 데이터 가시범위를 정의한다.
 */
@ManagedCode
public enum RowScope {

    OWN,
    ORG,
    ALL,
    CUSTOM;

    public boolean includesHierarchy() {
        return this == ORG || this == ALL;
    }

    public boolean isAllVisible() {
        return this == ALL;
    }

    public static RowScope of(String value) {
        return of(value, ALL);
    }

    public static RowScope of(String value, RowScope fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return RowScope.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
