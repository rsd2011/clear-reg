package com.example.common.identifier;

import com.example.common.masking.DataKind;
import com.example.common.masking.Maskable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 여권번호. 국가별 포맷 다양성 감안, 영숫자 6~9자로 제한. */
public final class PassportId implements Maskable<String> {
    private final String raw;

    private PassportId(String raw) {
        this.raw = raw;
    }

    @JsonCreator
    public static PassportId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PassportId must not be blank");
        }
        String trimmed = value.trim().toUpperCase();
        if (!trimmed.matches("[A-Z0-9]{6,9}")) {
            throw new IllegalArgumentException("PassportId must be 6-9 alphanumerics");
        }
        return new PassportId(trimmed);
    }

    @Override
    public String raw() { return raw; }

    @Override
    public String masked() {
        int visible = Math.min(3, raw.length());
        return "*".repeat(raw.length() - visible) + raw.substring(raw.length() - visible);
    }

    @Override
    public DataKind dataKind() {
        return DataKind.PASSPORT_NO;
    }

    @Override
    public String toString() { return masked(); }

    @JsonValue
    public String jsonValue() { return masked(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PassportId that)) return false;
        return raw.equals(that.raw);
    }

    @Override
    public int hashCode() { return raw.hashCode(); }
}
