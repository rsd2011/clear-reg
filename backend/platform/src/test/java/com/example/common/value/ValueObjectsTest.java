package com.example.common.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValueObjectsTest {

    @Test
    @DisplayName("Tokens are redacted in toString and json")
    void tokensAreRedacted() {
        SessionId sessionId = SessionId.of("session-123456");
        AuthToken authToken = AuthToken.of("a".repeat(32));
        FileToken fileToken = FileToken.of("filetoken-abcdef");

        assertThat(sessionId.toString()).contains("REDACTED");
        assertThat(authToken.toString()).contains("REDACTED");
        assertThat(fileToken.toString()).contains("REDACTED");
        assertThat(sessionId.raw()).contains("session");
    }

    @Test
    @DisplayName("MoneyAmount normalizes scale and currency")
    void moneyAmount() {
        MoneyAmount amount = MoneyAmount.of(new BigDecimal("123.456"), "usd");
        assertThat(amount.amount()).isEqualByComparingTo("123.46");
        assertThat(amount.currency().getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    @DisplayName("BirthDate rejects future dates")
    void birthDateValidation() {
        assertThatThrownBy(() -> BirthDate.of(LocalDate.now().plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
        BirthDate ok = BirthDate.of(LocalDate.of(1990, 1, 1));
        assertThat(ok.toString()).isEqualTo("1990-01-01");
    }

    @Test
    @DisplayName("Nationality, permission code and batch job validation")
    void codesValidation() {
        assertThat(NationalityCode.of("KR").value()).isEqualTo("KR");
        assertThatThrownBy(() -> NationalityCode.of("ZZ")).isInstanceOf(IllegalArgumentException.class);

        assertThat(PermissionGroupCode.of("ADMIN_READ").value()).isEqualTo("ADMIN_READ");
        assertThatThrownBy(() -> PermissionGroupCode.of("a"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(BatchJobId.of("JOB-20250101-0001").value()).contains("JOB");
        assertThatThrownBy(() -> BatchJobId.of("bad id")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Payment reference constraints")
    void paymentReference() {
        PaymentReference ref = PaymentReference.of("급여 이체 2025-01");
        assertThat(ref.raw()).contains("급여");
        assertThat(ref.toString()).contains("*");
        assertThatThrownBy(() -> PaymentReference.of("")).isInstanceOf(IllegalArgumentException.class);
    }
}
