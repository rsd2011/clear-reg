package com.example.common.value;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValueObjectsNegativeTest {

    @Test
    @DisplayName("MoneyAmount는 null 금액이나 빈 통화코드를 거부한다")
    void moneyAmountValidation() {
        assertThatThrownBy(() -> MoneyAmount.of(null, "USD")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MoneyAmount.of(BigDecimal.ONE, " ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("PaymentReference는 빈 문자열을 거부한다")
    void paymentReferenceValidation() {
        assertThatThrownBy(() -> PaymentReference.of(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}
