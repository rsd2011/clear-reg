package com.example.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.identifier.Address;
import com.example.common.identifier.PersonName;

class KoreanRomanizerTest {

    @Test
    @DisplayName("Romanize person name with capitalization")
    void romanizeName() {
        PersonName name = PersonName.of("홍길동");
        String result = KoreanRomanizer.romanize(name);
        assertThat(result.toLowerCase()).startsWith("hong").contains("gil").contains("dong");
    }

    @Test
    @DisplayName("Romanize structured address with country code kept")
    void romanizeAddress() {
        Address address = Address.of("KR", "Seoul", "강남구", "테헤란로 123", "삼성동 1-1", "06234");
        String result = KoreanRomanizer.romanizeAddress(address);
        assertThat(result.toLowerCase()).contains("gangnam").contains("teheran");
        assertThat(result).contains("KR");
    }
}
