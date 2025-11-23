package com.example.common.identifier;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IdentifierCoverageMoreTest {

    @Test
    @DisplayName("Customer/Account/Card/Phone/Email 마스킹 스모크")
    void customerAccountCardPhoneEmailMasking() {
        assertThat(CustomerId.of("CUST-ABCDE").masked()).endsWith("BCDE");
        assertThat(AccountId.of("110-123-456789").toString()).contains("*");
        assertThat(CardId.of("4111-1111-1111-1234").toString()).endsWith("1234");
        assertThat(PhoneNumber.of("010-9876-5432").toString()).endsWith("5432");
        assertThat(EmailAddress.of("user.name@example.com").toString()).contains("***");
    }

    @Test
    @DisplayName("주요 국가/생년월일/파일토큰 등 값 유지 확인")
    void nationalityBirthFileToken() {
        assertThat(com.example.common.value.NationalityCode.of("US").value()).isEqualTo("US");
        assertThat(com.example.common.value.BirthDate.of(java.time.LocalDate.of(2000, 1, 1)).toString()).isEqualTo("2000-01-01");
        assertThat(com.example.common.value.FileToken.of("file-token-abcdef").toString()).contains("REDACTED");
    }

    @Test
    @DisplayName("기타 ID들은 raw와 equals/hashCode를 유지한다")
    void otherIdsEquality() {
        CorporateRegistrationId corp = CorporateRegistrationId.of("123456-1234567");
        CorporateRegistrationId corp2 = CorporateRegistrationId.of("123456-1234567");
        assertThat(corp).isEqualTo(corp2);
        SecuritiesId sec = SecuritiesId.of("KR1234567890");
        assertThat(sec.raw()).isEqualTo("KR1234567890");
    }
}
