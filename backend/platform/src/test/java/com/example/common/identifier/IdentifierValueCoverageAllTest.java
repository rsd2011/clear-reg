package com.example.common.identifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.value.AuthToken;
import com.example.common.value.BatchJobId;
import com.example.common.value.FileToken;
import com.example.common.value.MoneyAmount;
import com.example.common.value.NationalityCode;
import com.example.common.value.PaymentReference;
import com.example.common.value.PermissionGroupCode;
import com.example.common.value.SessionId;

import java.math.BigDecimal;
import java.time.LocalDate;

class IdentifierValueCoverageAllTest {

    @Test
    @DisplayName("모든 식별자/값 객체의 happy-path 마스킹/원본/equals를 점검한다")
    void fullHappyPathSmoke() {
        // identifiers
        assertThat(CustomerId.of("CUST-ABCDE").raw()).isEqualTo("CUST-ABCDE");
        assertThat(CustomerId.of("CUST-ABCDE").masked()).contains("*");

        assertThat(EmployeeId.of("emp-12345").toString()).contains("emp");
        assertThat(OrganizationId.of("ORG-00123").toString()).startsWith("ORG");
        assertThat(AccountId.of("110-123-456789").masked()).endsWith("6789");
        assertThat(PassportId.of("M1234567").masked()).endsWith("567");
        assertThat(SecuritiesId.of("KR1234567890").masked()).endsWith("7890");
        assertThat(EmailAddress.of("user@example.com").masked()).contains("***");
        assertThat(OrganizationName.of("Acme Corp").value()).isEqualTo("Acme Corp");
        assertThat(DriverLicenseId.of("12-34-567890-12").masked()).contains("*");
        assertThat(BusinessRegistrationId.of("123-45-67890").masked()).endsWith("67890");
        assertThat(CorporateRegistrationId.of("123456-1234567").masked()).contains("*");
        assertThat(PersonName.of("홍길동").masked()).contains("*");
        assertThat(ResidentRegistrationId.of("990101-1234567").masked()).contains("*");
        assertThat(PhoneNumber.of("010-1234-5678").masked()).endsWith("5678");
        assertThat(CardId.of("4111-1111-1111-1234").masked()).endsWith("1234");
        assertThat(Address.of("KR", "Seoul", "Gangnam-gu", "Teheran-ro 1", "Unit", "06234").masked())
                .contains("[ADDR-REDACTED]");

        // equals/hashCode sanity
        assertThat(BusinessRegistrationId.of("123-45-67890"))
                .isEqualTo(BusinessRegistrationId.of("123-45-67890"));

        // values
        assertThat(SessionId.of("session-abcdef").toString()).contains("REDACTED");
        assertThat(AuthToken.of("a".repeat(32)).toString()).contains("REDACTED");
        assertThat(FileToken.of("file-token-abcdef").toString()).contains("REDACTED");
        assertThat(PaymentReference.of("급여 메모 1").masked()).contains("*");
        assertThat(PermissionGroupCode.of("AUDIT_VIEWER").value()).isEqualTo("AUDIT_VIEWER");
        assertThat(BatchJobId.of("JOB-20250101-0009").value()).contains("JOB");
        assertThat(NationalityCode.of("JP").value()).isEqualTo("JP");
        assertThat(com.example.common.value.BirthDate.of(LocalDate.of(1995, 5, 10)).toString()).isEqualTo("1995-05-10");
        assertThat(MoneyAmount.of(new BigDecimal("1234.56"), "USD").amount()).isEqualByComparingTo("1234.56");
    }

    @Test
    @DisplayName("일부 식별자는 잘못된 입력 시 예외를 던진다(추가 커버리지)")
    void additionalFailureBranches() {
        assertThatThrownBy(() -> AccountId.of("abc")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PassportId.of("M1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SecuritiesId.of("KR1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CardId.of("1111")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SessionId.of("short")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FileToken.of("short")).isInstanceOf(IllegalArgumentException.class);
    }
}
