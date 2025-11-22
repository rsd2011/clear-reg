package com.example.common.value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 배치/잡 식별자. */
public final class BatchJobId {
    private final String value;

    private BatchJobId(String value) {
        this.value = value;
    }

    @JsonCreator
    public static BatchJobId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("BatchJobId must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() < 6 || trimmed.length() > 80) {
            throw new IllegalArgumentException("BatchJobId length must be 6-80");
        }
        if (!trimmed.matches("[A-Za-z0-9\\-_:]+")) {
            throw new IllegalArgumentException("BatchJobId allows alnum, -, _, :");
        }
        return new BatchJobId(trimmed);
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
        if (!(o instanceof BatchJobId that)) return false;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
