package com.example.common.identifier;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IdentifierFailureTest {

    @Test
    @DisplayName("잘못된 포맷의 식별자는 예외를 던진다")
    void invalidIdentifiersThrow() {
        assertThatThrownBy(() -> BusinessRegistrationId.of("bad")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CorporateRegistrationId.of("bad")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PassportId.of("short")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SecuritiesId.of("KR1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DriverLicenseId.of("12-34")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CardId.of("1234")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AccountId.of("11")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EmailAddress.of("not-an-email")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PhoneNumber.of("12")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PersonName.of("1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Address.of("ZZ", "S", "C", "L1", null, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
