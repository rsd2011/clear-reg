package com.example.common.value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 통화 포함 금액 값 객체. 소수 2자리 반올림. */
public final class MoneyAmount {
    private final BigDecimal amount;
    private final Currency currency;

    private MoneyAmount(BigDecimal amount, Currency currency) {
        this.amount = amount;
        this.currency = currency;
    }

    @JsonCreator
    public static MoneyAmount of(@JsonProperty("amount") BigDecimal amount,
                                 @JsonProperty("currency") String currencyCode) {
        if (amount == null) {
            throw new IllegalArgumentException("amount must not be null");
        }
        if (currencyCode == null || currencyCode.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }
        Currency currency = Currency.getInstance(currencyCode.toUpperCase());
        int scale = Math.min(2, currency.getDefaultFractionDigits() < 0 ? 2 : currency.getDefaultFractionDigits());
        BigDecimal normalized = amount.setScale(scale, RoundingMode.HALF_UP);
        return new MoneyAmount(normalized, currency);
    }

    public BigDecimal amount() {
        return amount;
    }

    public Currency currency() {
        return currency;
    }

    @Override
    public String toString() {
        return currency.getCurrencyCode() + " " + amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MoneyAmount that)) return false;
        return amount.compareTo(that.amount) == 0 && currency.equals(that.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }
}
