package com.example.common.identifier;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IdentifierCoverageTest {

    @Test
    @DisplayName("주요 식별자는 masked()/toString()에서 민감 부분을 가린다")
    void maskedIdentifiers() {
        assertThat(CustomerId.of("CUST-12345").toString()).contains("*");
        assertThat(EmployeeId.of("emp-123").toString()).contains("emp");
        assertThat(OrganizationId.of("ORG-001").toString()).contains("ORG");
        assertThat(AccountId.of("110-123-456789").masked()).endsWith("6789");
        assertThat(CardId.of("4111-1111-1111-1234").masked()).endsWith("1234");
        assertThat(ResidentRegistrationId.of("990101-1234567").masked()).contains("*");
        assertThat(PassportId.of("M1234567").masked()).contains("*");
        assertThat(DriverLicenseId.of("12-34-567890-12").masked()).contains("*");
        assertThat(BusinessRegistrationId.of("123-45-67890").masked()).contains("67890");
        assertThat(CorporateRegistrationId.of("123456-1234567").masked()).contains("*");
        assertThat(SecuritiesId.of("KR1234567890").masked()).endsWith("7890");
        assertThat(EmailAddress.of("user@example.com").masked()).contains("***");
        assertThat(PhoneNumber.of("010-1234-5678").masked()).endsWith("5678");
        assertThat(PersonName.of("홍길동").masked()).contains("*");
        assertThat(OrganizationName.of("My Corp").value()).isEqualTo("My Corp");
        Address addr = Address.of("KR", "Seoul", "Gangnam", "Teheran-ro 1", "Unit", "06234");
        assertThat(addr.masked()).contains("[ADDR-REDACTED]").contains("Seoul");
    }
}
