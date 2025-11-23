package com.example.common.identifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IdentifierAdditionalTest {

    @Test
    void businessAndPersonalIdsMaskAndValidate() {
        assertThat(BusinessRegistrationId.of("123-45-67890").toString()).contains("67890");
        assertThat(CorporateRegistrationId.of("123456-1234567").toString()).contains("******");
        assertThat(OrganizationName.of("My Corp").value()).isEqualTo("My Corp");
    }

    @Test
    void passportsSecuritiesDriverPhoneEmail() {
        assertThat(PassportId.of("M1234567").masked()).endsWith("567");
        assertThat(SecuritiesId.of("KR1234567890").masked()).endsWith("7890");
        assertThat(DriverLicenseId.of("12-34-567890-12").masked()).endsWith("9012");
        assertThat(PhoneNumber.of("010-1234-5678").masked()).endsWith("5678");
        assertThat(EmailAddress.of("user@example.com").masked()).contains("u***@example.com");
    }

    @Test
    void cardAndAccountAndResident() {
        assertThat(CardId.of("4111-1111-1111-1234").masked()).endsWith("1234");
        assertThat(AccountId.of("110-123-456789").masked()).endsWith("6789");
        assertThat(ResidentRegistrationId.of("990101-1234567").masked()).contains("******");
    }

    @Test
    void addressAndPersonName() {
        Address address = Address.of("KR", "Seoul", "Gangnam", "Teheran-ro 1", "Unit", "06234");
        assertThat(address.countryCode()).isEqualTo("KR");
        assertThat(PersonName.of("홍길동").masked()).contains("*");
        assertThat(PersonName.of("Jane Doe").masked()).contains("J");
    }

    @Test
    void validationFailures() {
        assertThatThrownBy(() -> PassportId.of("bad")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PhoneNumber.of("1234")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EmailAddress.of("bad")).isInstanceOf(IllegalArgumentException.class);
    }
}
