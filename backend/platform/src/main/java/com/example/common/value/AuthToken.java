package com.example.common.value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 인증 토큰. toString/json은 항상 마스킹. */
public final class AuthToken {
    private static final String REDACTED = "[TOKEN-REDACTED]";
    private final String value;

    private AuthToken(String value) {
        this.value = value;
    }

    @JsonCreator
    public static AuthToken of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("AuthToken must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() < 16 || trimmed.length() > 4096) {
            throw new IllegalArgumentException("AuthToken length must be 16-4096");
        }
        return new AuthToken(trimmed);
    }

    public String raw() {
        return value;
    }

    @Override
    public String toString() {
        return REDACTED;
    }

    @JsonValue
    public String jsonValue() {
        return REDACTED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthToken that)) return false;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
