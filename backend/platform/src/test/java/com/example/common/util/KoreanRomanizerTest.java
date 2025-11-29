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

    @Test
    @DisplayName("주소 필드 일부가 null인 경우")
    void romanizeAddressPartialFields() {
        // stateOrProvince가 null인 경우
        Address noState = Address.of("KR", null, "서울시", "강남구", null, null);
        String noStateResult = KoreanRomanizer.romanizeAddress(noState);
        assertThat(noStateResult).isNotBlank();
        assertThat(noStateResult).contains("KR");

        // line2가 null인 경우
        Address noLine2 = Address.of("KR", "경기도", "수원시", "팔달구", null, "16300");
        String noLine2Result = KoreanRomanizer.romanizeAddress(noLine2);
        assertThat(noLine2Result).isNotBlank();

        // 모든 선택 필드가 null인 경우
        Address minimalAddr = Address.of("KR", null, "부산시", "해운대구", null, null);
        String minimalResult = KoreanRomanizer.romanizeAddress(minimalAddr);
        assertThat(minimalResult).contains("KR");
    }

    @Test
    @DisplayName("복합 주소 필드 조합")
    void romanizeAddressCombinations() {
        // city + stateOrProvince + countryCode
        Address fullAddr = Address.of("KR", "경기도", "서울시", "강남구", "역삼동", "06123");
        String fullResult = KoreanRomanizer.romanizeAddress(fullAddr);
        assertThat(fullResult).contains(", KR");

        // 다른 국가 코드
        Address usAddr = Address.of("US", "California", "Los Angeles", "Main Street", "Suite 100", "90001");
        String usResult = KoreanRomanizer.romanizeAddress(usAddr);
        assertThat(usResult).contains("US");

        // 한글 주소 로마자 변환
        Address koreanAddr = Address.of("KR", "경기도", "성남시", "분당구", "정자동", "13456");
        String koreanResult = KoreanRomanizer.romanizeAddress(koreanAddr);
        assertThat(koreanResult).isNotBlank();
    }
}

