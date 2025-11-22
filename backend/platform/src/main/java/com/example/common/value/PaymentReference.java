package com.example.common.value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 이체 메모/적요/참조 번호. */
import com.example.common.masking.Maskable;

public final class PaymentReference implements Maskable {
    private final String value;

    private PaymentReference(String value) {
        this.value = value;
    }

    private String maskedInternal() {
        int len = value.length();
        if (len <= 4) return "*".repeat(len);
        int visible = Math.min(2, len);
        return value.substring(0, visible) + "*".repeat(len - visible);
    }

    @JsonCreator
    public static PaymentReference of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PaymentReference must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() < 1 || trimmed.length() > 140) {
            throw new IllegalArgumentException("PaymentReference length must be 1-140");
        }
        if (!trimmed.matches("[\\p{L}\\p{N} .,'\"()\\-_/]*")) {
            throw new IllegalArgumentException("PaymentReference contains invalid characters");
        }
        return new PaymentReference(trimmed);
    }

    public String value() {
        return value;
    }

    @Override
    public String raw() {
        return value;
    }

    @Override
    public String masked() { return maskedInternal(); }

    @Override
    public String toString() {
        return maskedInternal();
    }

    @JsonValue
    public String jsonValue() {
        return maskedInternal();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaymentReference that)) return false;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
