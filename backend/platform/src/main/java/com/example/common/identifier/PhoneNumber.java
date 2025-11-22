package com.example.common.identifier;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 전화번호 (국내외). 숫자만 저장, 기본 마스킹. */
public final class PhoneNumber {

    private final String digits;

    private PhoneNumber(String digits) {
        this.digits = digits;
    }

    @JsonCreator
    public static PhoneNumber of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PhoneNumber must not be blank");
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.length() < 9 || digits.length() > 12) {
            throw new IllegalArgumentException("PhoneNumber must be 9-12 digits");
        }
        return new PhoneNumber(digits);
    }

    public String raw() { return digits; }

    public String masked() {
        if (digits.length() <= 4) return digits;
        String tail = digits.substring(digits.length() - 4);
        return "***" + tail;
    }

    @Override
    public String toString() { return masked(); }

    @JsonValue
    public String jsonValue() { return masked(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhoneNumber that)) return false;
        return digits.equals(that.digits);
    }

    @Override
    public int hashCode() { return digits.hashCode(); }
}
