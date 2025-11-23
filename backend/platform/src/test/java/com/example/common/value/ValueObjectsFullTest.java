package com.example.common.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValueObjectsFullTest {

    @Test
    @DisplayName("Session/Auth/File 토큰은 toString/json에서 마스킹된다")
    void tokensMasking() {
        SessionId session = SessionId.of("session-abcdef");
        AuthToken token = AuthToken.of("a".repeat(32));
        FileToken file = FileToken.of("file-token-123456");
        assertThat(session.toString()).contains("REDACTED");
        assertThat(token.toString()).contains("REDACTED");
        assertThat(file.toString()).contains("REDACTED");
    }

    @Test
    @DisplayName("MoneyAmount는 통화 자리수에 맞춰 반올림한다")
    void moneyAmountRounding() {
        MoneyAmount krw = MoneyAmount.of(new BigDecimal("1000.19"), "krw");
        assertThat(krw.amount()).isEqualByComparingTo("1000");
        MoneyAmount usd = MoneyAmount.of(new BigDecimal("12.345"), "USD");
        assertThat(usd.amount()).isEqualByComparingTo("12.35");
    }

    @Test
    @DisplayName("기타 값 객체 생성 및 검증")
    void others() {
        PaymentReference ref = PaymentReference.of("급여 메모 1");
        assertThat(ref.masked()).contains("*");

        PermissionGroupCode pg = PermissionGroupCode.of("AUDIT_VIEWER");
        BatchJobId job = BatchJobId.of("JOB-20250101-0001");
        NationalityCode nation = NationalityCode.of("KR");
        BirthDate birth = BirthDate.of(LocalDate.of(1990, 1, 1));

        assertThat(pg.value()).isEqualTo("AUDIT_VIEWER");
        assertThat(job.value()).contains("JOB");
        assertThat(nation.value()).isEqualTo("KR");
        assertThat(birth.toString()).isEqualTo("1990-01-01");

        assertThatThrownBy(() -> FileToken.of("short")).isInstanceOf(IllegalArgumentException.class);
    }
}
