package com.example.common.value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Locale;

/** ISO 3166-1 alpha-2 국가 코드. */
public final class NationalityCode {
    private final String value;

    private NationalityCode(String value) {
        this.value = value;
    }

    @JsonCreator
    public static NationalityCode of(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("NationalityCode must not be blank");
        }
        String upper = code.trim().toUpperCase(Locale.ROOT);
        boolean valid = Arrays.stream(Locale.getISOCountries()).anyMatch(upper::equals);
        if (!valid) {
            throw new IllegalArgumentException("Invalid country code: " + code);
        }
        return new NationalityCode(upper);
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
        if (!(o instanceof NationalityCode that)) return false;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
