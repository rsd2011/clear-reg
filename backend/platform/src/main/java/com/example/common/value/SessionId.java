package com.example.common.value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 세션 식별자. 로깅 시 노출 방지를 위해 toString/json은 마스킹한다. */
public final class SessionId {
    private static final String REDACTED = "[SESSION-REDACTED]";
    private final String value;

    private SessionId(String value) {
        this.value = value;
    }

    @JsonCreator
    public static SessionId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SessionId must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() < 8 || trimmed.length() > 64) {
            throw new IllegalArgumentException("SessionId length must be 8-64");
        }
        if (!trimmed.matches("[A-Za-z0-9._\\-]+")) {
            throw new IllegalArgumentException("SessionId contains invalid characters");
        }
        return new SessionId(trimmed);
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
        if (!(o instanceof SessionId that)) return false;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
