package com.example.admin.codegroup.locale.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * ISO 3166-1 국가 코드 메타데이터.
 *
 * <p>국가 코드의 추가 정보를 저장합니다.</p>
 *
 * @param alpha3 ISO 3166-1 alpha-3 코드 (예: "KOR")
 * @param numeric ISO 3166-1 numeric 코드 (예: "410")
 * @param englishName 영문명 (예: "Korea, Republic of")
 * @param custom 커스텀 추가 항목 여부 (ISO 항목은 null)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LocaleCountryMetadata(
        String alpha3,
        String numeric,
        String englishName,
        Boolean custom
) {

    /**
     * ISO 국가 메타데이터 생성
     */
    public static LocaleCountryMetadata ofIso(String alpha3, String numeric, String englishName) {
        return new LocaleCountryMetadata(alpha3, numeric, englishName, null);
    }

    /**
     * 커스텀 국가 메타데이터 생성
     */
    public static LocaleCountryMetadata ofCustom(String alpha3, String numeric, String englishName) {
        return new LocaleCountryMetadata(alpha3, numeric, englishName, true);
    }
}
