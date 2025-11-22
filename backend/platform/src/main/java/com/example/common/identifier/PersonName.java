package com.example.common.identifier;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 사람 이름 값 객체. 한국어 2~6자 또는 영문/공백 2~50자 허용.
 * toString()/Json 직렬화 시 앞 1~2자만 노출하고 나머지는 마스킹한다.
 */
public final class PersonName {

    private final String raw;

    private PersonName(String raw) {
        this.raw = raw;
    }

    @JsonCreator
    public static PersonName of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PersonName must not be blank");
        }
        String trimmed = value.trim();
        boolean korean = trimmed.matches("[가-힣]{2,6}");
        boolean latin = trimmed.matches("[A-Za-z][A-Za-z .'-]{1,49}");
        if (!korean && !latin) {
            throw new IllegalArgumentException("PersonName must be 2-6 Korean chars or 2-50 Latin chars");
        }
        return new PersonName(trimmed);
    }

    public String raw() {
        return raw;
    }

    public String masked() {
        int len = raw.length();
        if (len == 1) return raw;
        if (len == 2) return raw.charAt(0) + "*";
        // len >=3 : 앞 1자 + 가운데 마스킹 + 끝 1자
        return raw.charAt(0) + "*".repeat(len - 2) + raw.charAt(len - 1);
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
        if (!(o instanceof PersonName that)) return false;
        return raw.equals(that.raw);
    }

    @Override
    public int hashCode() {
        return raw.hashCode();
    }
}
