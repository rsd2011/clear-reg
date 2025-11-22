package com.example.common.identifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IdentifierTest {

    @Test
    @DisplayName("CustomerId masks in toString while keeping raw()")
    void customerIdMasks() {
        CustomerId id = CustomerId.of("CUST123456");
        assertThat(id.raw()).isEqualTo("CUST123456");
        assertThat(id.toString()).isEqualTo("******3456");
        assertThat(id.masked()).isEqualTo("******3456");
    }

    @Test
    @DisplayName("EmployeeId validates allowed characters and length")
    void employeeIdValidates() {
        assertThatThrownBy(() -> EmployeeId.of("  "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EmployeeId.of("한글"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EmployeeId.of("abc"))
                .isInstanceOf(IllegalArgumentException.class);

        EmployeeId id = EmployeeId.of("emp-001");
        assertThat(id.raw()).isEqualTo("emp-001");
    }

    @Test
    @DisplayName("OrganizationId equality uses raw value, not masked")
    void organizationIdEquality() {
        OrganizationId a = OrganizationId.of("ORG9999");
        OrganizationId b = OrganizationId.of("ORG9999");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("Masking keeps last up-to-4 characters only")
    void maskingLastFour() {
        CustomerId shortId = CustomerId.of("ABCD");
        assertThat(shortId.masked()).isEqualTo("ABCD");

        CustomerId longId = CustomerId.of("ABCDEFGHIJK");
        assertThat(longId.masked()).isEqualTo("*******HIJK");
    }

    @Test
    @DisplayName("Sensitive identifiers mask appropriately")
    void sensitiveIdentifierMasking() {
        assertThat(ResidentRegistrationId.of("990101-1234567").toString()).isEqualTo("******-*******");
        assertThat(CardId.of("4111-1111-1111-1234").toString()).isEqualTo("************1234");
        assertThat(AccountId.of("110-123-456789").toString()).isEqualTo("********6789");
        assertThat(BusinessRegistrationId.of("123-45-67890").toString()).isEqualTo("***-**-67890");
        assertThat(CorporateRegistrationId.of("123456-1234567").toString()).isEqualTo("******-*******");
        assertThat(PassportId.of("M1234567").toString()).isEqualTo("*****567");
        assertThat(DriverLicenseId.of("12-34-567890-12").toString()).isEqualTo("********9012");
        assertThat(PhoneNumber.of("010-1234-5678").toString()).isEqualTo("***5678");
        assertThat(EmailAddress.of("user.name@example.com").toString()).isEqualTo("u***@example.com");
        assertThat(PersonName.of("홍길동").toString()).isEqualTo("홍*동");
        assertThat(PersonName.of("Jane Doe").toString()).isEqualTo("J******e");
        assertThat(SecuritiesId.of("KR1234567890").toString()).isEqualTo("********7890");
        Address address = Address.of("KR", "Seoul", "Gangnam-gu", "Teheran-ro 123", "Unit 101", "06234");
        assertThat(address.toString()).contains("[ADDR-REDACTED]").contains("Gangnam-gu").contains("KR");
    }

    @Test
    @DisplayName("Email and phone validation rejects bad input")
    void validation() {
        assertThatThrownBy(() -> EmailAddress.of("bad-email")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PhoneNumber.of("12345")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CardId.of("1111")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PersonName.of("1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SecuritiesId.of("abc")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Address.of("ZZ", null, "City", "Street", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Address.of("US", null, "C", "Street", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OrganizationName.of("!@#")).isInstanceOf(IllegalArgumentException.class);
    }
}
