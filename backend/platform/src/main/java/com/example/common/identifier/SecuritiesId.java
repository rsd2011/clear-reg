package com.example.common.identifier;

import com.example.common.masking.DataKind;
import com.example.common.masking.Maskable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 증권/종목/계좌 식별자 (예: ISIN, 종목코드 등). 영숫자 6~20자.
 */
public final class SecuritiesId implements Maskable<String> {

    private final String raw;

    private SecuritiesId(String raw) {
        this.raw = raw;
    }

    @JsonCreator
    public static SecuritiesId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SecuritiesId must not be blank");
        }
        String trimmed = value.trim().toUpperCase();
        if (!trimmed.matches("[A-Z0-9]{6,20}")) {
            throw new IllegalArgumentException("SecuritiesId must be 6-20 alphanumerics");
        }
        return new SecuritiesId(trimmed);
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
        return DataKind.SECURITIES_NO;
    }

    @Override
    public String toString() { return masked(); }

    @JsonValue
    public String jsonValue() { return masked(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SecuritiesId that)) return false;
        return raw.equals(that.raw);
    }

    @Override
    public int hashCode() { return raw.hashCode(); }
}
