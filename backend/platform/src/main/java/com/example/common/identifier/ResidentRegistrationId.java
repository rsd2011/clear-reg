package com.example.common.identifier;

import com.example.common.masking.DataKind;
import com.example.common.masking.Maskable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 주민등록번호 (주민번호) 전용 값 객체. */
public final class ResidentRegistrationId implements Maskable<String> {

    private static final String MASKED = "******-*******";
    private final String raw;

    private ResidentRegistrationId(String raw) {
        this.raw = raw;
    }

    @JsonCreator
    public static ResidentRegistrationId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ResidentRegistrationId must not be blank");
        }
        String trimmed = value.trim();
        if (!trimmed.matches("\\d{6}-\\d{7}")) {
            throw new IllegalArgumentException("ResidentRegistrationId must match 6 digits, dash, 7 digits");
        }
        return new ResidentRegistrationId(trimmed);
    }

    @Override
    public String raw() {
        return raw;
    }

    @Override
    public String masked() {
        return MASKED;
    }

    @Override
    public DataKind dataKind() {
        return DataKind.SSN;
    }

    @Override
    public String toString() {
        return MASKED;
    }

    @JsonValue
    public String jsonValue() {
        return MASKED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResidentRegistrationId that = (ResidentRegistrationId) o;
        return raw.equals(that.raw);
    }

    @Override
    public int hashCode() {
        return raw.hashCode();
    }
}
