package com.example.common.identifier;

import com.example.common.masking.DataKind;
import com.example.common.masking.Maskable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 사업자등록번호(10자리). */
public final class BusinessRegistrationId implements Maskable<String> {

    private final String digits;

    private BusinessRegistrationId(String digits) {
        this.digits = digits;
    }

    @JsonCreator
    public static BusinessRegistrationId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("BusinessRegistrationId must not be blank");
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.length() != 10) {
            throw new IllegalArgumentException("BusinessRegistrationId must be 10 digits");
        }
        return new BusinessRegistrationId(digits);
    }

    @Override
    public String raw() {
        return digits;
    }

    @Override
    public String masked() {
        return "***-**-" + digits.substring(5);
    }

    @Override
    public DataKind dataKind() {
        return DataKind.BUSINESS_REG_NO;
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
        BusinessRegistrationId that = (BusinessRegistrationId) o;
        return digits.equals(that.digits);
    }

    @Override
    public int hashCode() {
        return digits.hashCode();
    }
}
