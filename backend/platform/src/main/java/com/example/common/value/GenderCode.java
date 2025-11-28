package com.example.common.value;

import com.example.common.codegroup.annotation.ManagedCode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 간단한 성별 코드. */
@ManagedCode
public enum GenderCode {
    MALE, FEMALE, OTHER, UNKNOWN;

    @JsonCreator
    public static GenderCode from(String value) {
        if (value == null || value.isBlank()) return UNKNOWN;
        try {
            return GenderCode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }

    @JsonValue
    public String jsonValue() {
        return name();
    }
}
