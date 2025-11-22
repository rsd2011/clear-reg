package com.example.common.identifier;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 계좌 번호 값 객체. */
public final class AccountId {

    private final String digits;

    private AccountId(String digits) {
        this.digits = digits;
    }

    @JsonCreator
    public static AccountId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("AccountId must not be blank");
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.length() < 10 || digits.length() > 16) {
            throw new IllegalArgumentException("AccountId length must be 10~16 digits");
        }
        return new AccountId(digits);
    }

    public String raw() {
        return digits;
    }

    public String masked() {
        int len = digits.length();
        int visible = Math.min(4, len);
        return "*".repeat(len - visible) + digits.substring(len - visible);
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
        AccountId accountId = (AccountId) o;
        return digits.equals(accountId.digits);
    }

    @Override
    public int hashCode() {
        return digits.hashCode();
    }
}
