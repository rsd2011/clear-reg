package com.example.common.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("값 객체 커버리지 보강")
class ValueComprehensiveCoverageTest {

    @Test
    @DisplayName("세션/토큰/권한/배치/파일 토큰 정상·실패 경로")
    void tokenAndCodes() {
        SessionId session = SessionId.of("SESSION_1234");
        assertThat(session.raw()).isEqualTo("SESSION_1234");
        assertThat(session.toString()).isEqualTo("[SESSION-REDACTED]");
        assertThat(session).isEqualTo(session);
        assertThat(session).isNotEqualTo(null);
        assertThrows(IllegalArgumentException.class, () -> SessionId.of("bad"));

        AuthToken token = AuthToken.of("a".repeat(32));
        assertThat(token.raw()).hasSize(32);
        assertThat(token.toString()).contains("REDACTED");
        assertThat(token).isEqualTo(token);
        assertThat(token).isNotEqualTo(null);
        assertThrows(IllegalArgumentException.class, () -> AuthToken.of("short"));

        PermissionGroupCode pg = PermissionGroupCode.of("PERM_VIEW");
        assertThat(pg.value()).isEqualTo("PERM_VIEW");
        assertThrows(IllegalArgumentException.class, () -> PermissionGroupCode.of(""));
        assertThrows(IllegalArgumentException.class, () -> PermissionGroupCode.of("!invalid"));
        assertThat(pg).isEqualTo(PermissionGroupCode.of("perm_view"));

        BatchJobId job = BatchJobId.of("JOB-1234");
        assertThat(job.value()).isEqualTo("JOB-1234");
        assertThrows(IllegalArgumentException.class, () -> BatchJobId.of("bad!"));
        assertThrows(IllegalArgumentException.class, () -> BatchJobId.of("short"));
        assertThat(job).isEqualTo(BatchJobId.of("JOB-1234"));
        assertThat(job).isNotEqualTo(null);

        FileToken fileToken = FileToken.of("FILE-TOKEN-XYZ");
        assertThat(fileToken.raw()).isEqualTo("FILE-TOKEN-XYZ");
        assertThrows(IllegalArgumentException.class, () -> FileToken.of("   "));
        assertThrows(IllegalArgumentException.class, () -> FileToken.of("tiny"));
        assertThat(fileToken).isEqualTo(FileToken.of("FILE-TOKEN-XYZ"));
        assertThat(fileToken).isNotEqualTo(FileToken.of("FILE-TOKEN-ABC"));
    }

    @Test
    @DisplayName("금액/통화, 지불참조, 국적/생년월일 검증")
    void moneyAndPersonValues() {
        MoneyAmount usd = MoneyAmount.of(new BigDecimal("1234.567"), "USD");
        assertThat(usd.amount().toPlainString()).isEqualTo("1234.57");
        assertThat(usd.currency().getCurrencyCode()).isEqualTo("USD");
        assertThat(usd.toString()).contains("USD");
        assertThat(usd).isEqualTo(MoneyAmount.of(new BigDecimal("1234.57"), "USD"));
        MoneyAmount jpy = MoneyAmount.of(new BigDecimal("1000.4"), "JPY");
        assertThat(jpy.amount().scale()).isEqualTo(0);
        assertThat(usd).isNotEqualTo(jpy);
        assertThrows(IllegalArgumentException.class, () -> MoneyAmount.of(null, "USD"));
        assertThrows(IllegalArgumentException.class, () -> MoneyAmount.of(BigDecimal.ONE, ""));
        assertThrows(IllegalArgumentException.class, () -> MoneyAmount.of(BigDecimal.ONE, "ZZ"));

        PaymentReference ref = PaymentReference.of("송금메모1234");
        assertThat(ref.masked()).contains("송");
        assertThrows(IllegalArgumentException.class, () -> PaymentReference.of(""));
        assertThrows(IllegalArgumentException.class, () -> PaymentReference.of("<tag>"));
        PaymentReference shortRef = PaymentReference.of("abcd");
        assertThat(shortRef.masked()).isEqualTo("****");
        assertThat(ref).isEqualTo(PaymentReference.of("송금메모1234"));
        assertThat(ref).isNotEqualTo(shortRef);

        NationalityCode nationality = NationalityCode.of("kr");
        assertThat(nationality.value()).isEqualTo("KR");
        assertThrows(IllegalArgumentException.class, () -> NationalityCode.of("ZZZ"));
        assertThat(nationality).isEqualTo(NationalityCode.of("KR"));
        assertThat(nationality.jsonValue()).isEqualTo("KR");
        assertThat(nationality).isNotEqualTo(null);

        BirthDate birth = BirthDate.of(LocalDate.of(1990, 1, 1));
        assertThat(birth.value()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> BirthDate.of(null));
        assertThrows(IllegalArgumentException.class, () -> BirthDate.of(LocalDate.now().plusDays(1)));
        assertThrows(IllegalArgumentException.class, () -> BirthDate.of(LocalDate.of(1800, 1, 1)));
        assertThat(birth).isEqualTo(BirthDate.of(LocalDate.of(1990, 1, 1)));
        assertThat(birth.toString()).contains("1990-01-01");
    }

    @Test
    @DisplayName("세션/토큰 동등성 및 해시")
    void equalityAndHash() {
        SessionId s1 = SessionId.of("SESSION_1234");
        SessionId s2 = SessionId.of("SESSION_1234");
        assertThat(s1).isEqualTo(s2);
        assertThat(s1.hashCode()).isEqualTo(s2.hashCode());

        AuthToken t1 = AuthToken.of("b".repeat(24));
        AuthToken t2 = AuthToken.of("b".repeat(24));
        assertThat(t1).isEqualTo(t2);
        assertThat(t1.hashCode()).isEqualTo(t2.hashCode());
    }
}
