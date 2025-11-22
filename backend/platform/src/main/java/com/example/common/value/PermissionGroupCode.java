package com.example.common.value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 권한 그룹 코드 (RBAC/ABAC). */
public final class PermissionGroupCode {
    private final String value;

    private PermissionGroupCode(String value) {
        this.value = value;
    }

    @JsonCreator
    public static PermissionGroupCode of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PermissionGroupCode must not be blank");
        }
        String trimmed = value.trim().toUpperCase();
        if (trimmed.length() < 2 || trimmed.length() > 64) {
            throw new IllegalArgumentException("PermissionGroupCode length must be 2-64");
        }
        if (!trimmed.matches("[A-Z0-9_\\-]+")) {
            throw new IllegalArgumentException("PermissionGroupCode allows A-Z,0-9,_,-");
        }
        return new PermissionGroupCode(trimmed);
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @JsonValue
    public String jsonValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PermissionGroupCode that)) return false;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
