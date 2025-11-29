package com.example.common.identifier;

import com.example.common.masking.DataKind;
import com.example.common.masking.Maskable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 이메일 주소. 로컬 파트 마스킹. */
public final class EmailAddress implements Maskable<String> {

    private final String raw;

    private EmailAddress(String raw) {
        this.raw = raw;
    }

    @JsonCreator
    public static EmailAddress of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("EmailAddress must not be blank");
        }
        String trimmed = value.trim();
        if (!trimmed.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("EmailAddress invalid format");
        }
        return new EmailAddress(trimmed);
    }

    @Override
    public String raw() { return raw; }

    @Override
    public String masked() {
        int at = raw.indexOf('@');
        String local = raw.substring(0, at);
        String domain = raw.substring(at);
        String visible = local.isEmpty() ? "" : local.substring(0, 1);
        return visible + "***" + domain;
    }

    @Override
    public DataKind dataKind() {
        return DataKind.EMAIL;
    }

    @Override
    public String toString() { return masked(); }

    @JsonValue
    public String jsonValue() { return masked(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmailAddress that)) return false;
        return raw.equalsIgnoreCase(that.raw);
    }

    @Override
    public int hashCode() { return raw.toLowerCase().hashCode(); }
}
