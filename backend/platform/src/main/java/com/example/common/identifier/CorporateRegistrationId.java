package com.example.common.identifier;

import com.example.common.masking.DataKind;
import com.example.common.masking.Maskable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 법인등록번호(13자리). */
public final class CorporateRegistrationId implements Maskable<String> {

    private final String digits;

    private CorporateRegistrationId(String digits) {
        this.digits = digits;
    }

    @JsonCreator
    public static CorporateRegistrationId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CorporateRegistrationId must not be blank");
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.length() != 13) {
            throw new IllegalArgumentException("CorporateRegistrationId must be 13 digits");
        }
        return new CorporateRegistrationId(digits);
    }

    @Override
    public String raw() {
        return digits;
    }

    @Override
    public String masked() {
        return "******-*******";
    }

    @Override
    public DataKind dataKind() {
        return DataKind.CORP_REG_NO;
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
        CorporateRegistrationId that = (CorporateRegistrationId) o;
        return digits.equals(that.digits);
    }

    @Override
    public int hashCode() {
        return digits.hashCode();
    }
}
