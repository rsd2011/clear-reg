package com.example.common.identifier;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 조직/법인/사업체 이름. 마스킹 없이 검증·정규화만 담당.
 */
public final class OrganizationName {

    private final String value;

    private OrganizationName(String value) {
        this.value = value;
    }

    @JsonCreator
    public static OrganizationName of(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("OrganizationName must not be blank");
        }
        String trimmed = name.trim();
        if (trimmed.length() < 2 || trimmed.length() > 120) {
            throw new IllegalArgumentException("OrganizationName length must be 2-120");
        }
        if (!trimmed.matches("[\\p{L}\\p{N} .,&'’\"()\\-_/]+")) {
            throw new IllegalArgumentException("OrganizationName contains invalid characters");
        }
        return new OrganizationName(trimmed);
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
        if (!(o instanceof OrganizationName that)) return false;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
