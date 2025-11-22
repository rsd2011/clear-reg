package com.example.common.value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 파일 다운로드/DRM 토큰. 로깅 시 마스킹. */
public final class FileToken {
    private static final String REDACTED = "[FILE-TOKEN-REDACTED]";
    private final String value;

    private FileToken(String value) {
        this.value = value;
    }

    @JsonCreator
    public static FileToken of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("FileToken must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() < 12 || trimmed.length() > 256) {
            throw new IllegalArgumentException("FileToken length must be 12-256");
        }
        return new FileToken(trimmed);
    }

    public String raw() {
        return value;
    }

    @Override
    public String toString() {
        return REDACTED;
    }

    @JsonValue
    public String jsonValue() {
        return REDACTED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileToken that)) return false;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
