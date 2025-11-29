package com.example.common.identifier;

import com.example.common.masking.DataKind;
import com.example.common.masking.Maskable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 운전면허번호 (지역코드/연도 포함 가능) */
public final class DriverLicenseId implements Maskable<String> {

    private final String raw;

    private DriverLicenseId(String raw) {
        this.raw = raw;
    }

    @JsonCreator
    public static DriverLicenseId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("DriverLicenseId must not be blank");
        }
        String normalized = value.replaceAll("[^0-9]", "");
        if (normalized.length() < 10 || normalized.length() > 12) {
            throw new IllegalArgumentException("DriverLicenseId must be 10-12 digits");
        }
        return new DriverLicenseId(normalized);
    }

    @Override
    public String raw() { return raw; }

    @Override
    public String masked() {
        int visible = Math.min(4, raw.length());
        return "*".repeat(raw.length() - visible) + raw.substring(raw.length() - visible);
    }

    @Override
    public DataKind dataKind() {
        return DataKind.DRIVER_LICENSE;
    }

    @Override
    public String toString() { return masked(); }

    @JsonValue
    public String jsonValue() { return masked(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DriverLicenseId that)) return false;
        return raw.equals(that.raw);
    }

    @Override
    public int hashCode() { return raw.hashCode(); }
}
