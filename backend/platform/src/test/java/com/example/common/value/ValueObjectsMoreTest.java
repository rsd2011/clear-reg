package com.example.common.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class ValueObjectsMoreTest {

    @Test
    void tokensAndCodes() {
        SessionId session = SessionId.of("session-xyz");
        AuthToken token = AuthToken.of("a".repeat(32));
        FileToken fileToken = FileToken.of("file-token-1234");
        GenderCode gender = GenderCode.from("MALE");

        assertThat(session.toString()).contains("REDACTED");
        assertThat(token.toString()).contains("REDACTED");
        assertThat(fileToken.toString()).contains("REDACTED");
        assertThat(gender.jsonValue()).isEqualTo("MALE");
    }

    @Test
    void monetaryAndReference() {
        MoneyAmount amount = MoneyAmount.of(new BigDecimal("1000.1"), "KRW");
        assertThat(amount.amount()).isEqualByComparingTo("1000"); // KRW는 소수 자릿수 0
        PaymentReference ref = PaymentReference.of("급여 메모");
        assertThat(ref.masked()).contains("*");
    }

    @Test
    void codesValidate() {
        PermissionGroupCode pg = PermissionGroupCode.of("AUDIT_VIEWER");
        BatchJobId jobId = BatchJobId.of("JOB-20250101-0002");
        assertThat(pg.value()).isEqualTo("AUDIT_VIEWER");
        assertThat(jobId.value()).contains("JOB");
        assertThat(GenderCode.from("X")).isEqualTo(GenderCode.UNKNOWN);
    }

    @Test
    void nationalityAndBirthDate() {
        NationalityCode nc = NationalityCode.of("US");
        BirthDate bd = BirthDate.of(LocalDate.of(1980, 1, 1));
        assertThat(nc.value()).isEqualTo("US");
        assertThat(bd.toString()).isEqualTo("1980-01-01");
        assertThatThrownBy(() -> BirthDate.of(LocalDate.now().plusDays(1))).isInstanceOf(IllegalArgumentException.class);
    }
}
