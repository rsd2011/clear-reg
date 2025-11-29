package com.example.common.identifier;

import com.example.common.masking.DataKind;
import com.example.common.masking.Maskable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 내부/외부 고객 식별자.
 */
public final class CustomerId extends AbstractIdentifier implements Maskable<String> {

    private final String raw;

    private CustomerId(String raw) {
        this.raw = raw;
    }

    @JsonCreator
    public static CustomerId of(String value) {
        String normalized = normalizeAndValidate(value, "CustomerId");
        return new CustomerId(normalized);
    }

    @Override
    public String raw() {
        return raw;
    }

    @Override
    public String masked() {
        return mask(raw);
    }

    @Override
    public DataKind dataKind() {
        return DataKind.CUSTOMER_ID;
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
