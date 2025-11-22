package com.example.common.identifier;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 내부/외부 고객 식별자.
 */
public final class CustomerId extends AbstractIdentifier {

    private final String raw;

    private CustomerId(String raw) {
        this.raw = raw;
    }

    @JsonCreator
    public static CustomerId of(String value) {
        String normalized = normalizeAndValidate(value, "CustomerId");
        return new CustomerId(normalized);
    }

    public String raw() {
        return raw;
    }

    public String masked() {
        return mask(raw);
    }

    @Override
    public String toString() {
        return masked();
    }

    @JsonValue
    public String jsonValue() {
        return masked();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerId that = (CustomerId) o;
        return raw.equals(that.raw);
    }

    @Override
    public int hashCode() {
        return raw.hashCode();
    }
}
