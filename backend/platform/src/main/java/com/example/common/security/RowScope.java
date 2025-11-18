package com.example.common.security;

/**
 * 행 단위 데이터 가시범위를 정의한다.
 */
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
}
