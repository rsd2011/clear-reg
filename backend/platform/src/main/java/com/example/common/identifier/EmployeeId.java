package com.example.common.identifier;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 임직원(내부 사용자) 식별자.
 */
public final class EmployeeId extends AbstractIdentifier {

    private final String raw;

    private EmployeeId(String raw) {
        this.raw = raw;
    }

    @JsonCreator
    public static EmployeeId of(String value) {
        String normalized = normalizeAndValidate(value, "EmployeeId");
        return new EmployeeId(normalized);
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
        EmployeeId that = (EmployeeId) o;
        return raw.equals(that.raw);
    }

    @Override
    public int hashCode() {
        return raw.hashCode();
    }
}
