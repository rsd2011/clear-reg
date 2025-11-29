package com.example.common.identifier;

import com.example.common.masking.DataKind;
import com.example.common.masking.Maskable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 카드 번호 (PAN) 값 객체. 숫자만 보관, 마스킹 기본. */
public final class CardId implements Maskable<String> {

    private final String digits; // normalized digits only

    private CardId(String digits) {
        this.digits = digits;
    }

    @JsonCreator
    public static CardId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CardId must not be blank");
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.length() < 13 || digits.length() > 19) {
            throw new IllegalArgumentException("CardId length must be 13~19 digits");
        }
        return new CardId(digits);
    }

    @Override
    public String raw() {
        return digits;
    }

    @Override
    public String masked() {
        int len = digits.length();
        int visible = Math.min(4, len);
        return "*".repeat(len - visible) + digits.substring(len - visible);
    }

    @Override
    public DataKind dataKind() {
        return DataKind.CARD_NO;
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
        CardId cardId = (CardId) o;
        return digits.equals(cardId.digits);
    }

    @Override
    public int hashCode() {
        return digits.hashCode();
    }
}
