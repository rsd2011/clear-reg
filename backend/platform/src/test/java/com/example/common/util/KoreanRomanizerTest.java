package com.example.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.identifier.Address;
import com.example.common.identifier.PersonName;

@DisplayName("KoreanRomanizer 커버리지")
class KoreanRomanizerTest {

    @Test
    @DisplayName("이름/주소 로마자 변환 및 null 처리")
    void romanizeBranches() {
        assertThat(KoreanRomanizer.romanize(null)).isNull();
        PersonName name = PersonName.of("홍길동");
        assertThat(KoreanRomanizer.romanize(name)).isNotBlank();

        Address address = Address.of("KR", "Seoul", "Gangnam", "Teheran-ro 1", "Apt 101", "06236");
        assertThat(KoreanRomanizer.romanizeAddress(address)).contains("KR");
        assertThat(KoreanRomanizer.romanizeAddress(null)).isNull();
    }
}

