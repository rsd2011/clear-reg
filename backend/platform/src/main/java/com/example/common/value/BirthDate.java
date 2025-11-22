package com.example.common.value;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 생년월일. 미래 날짜 금지. */
public final class BirthDate {
    private final LocalDate value;

    private BirthDate(LocalDate value) {
        this.value = value;
    }

    @JsonCreator
    public static BirthDate of(LocalDate value) {
        if (value == null) {
            throw new IllegalArgumentException("BirthDate must not be null");
        }
        if (value.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("BirthDate cannot be in the future");
        }
        if (value.isBefore(LocalDate.of(1900, 1, 1))) {
            throw new IllegalArgumentException("BirthDate is unrealistically old");
        }
        return new BirthDate(value);
    }

    public LocalDate value() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @JsonValue
    public String jsonValue() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BirthDate that)) return false;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
