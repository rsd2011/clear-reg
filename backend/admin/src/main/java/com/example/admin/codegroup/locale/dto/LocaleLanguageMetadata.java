package com.example.admin.codegroup.locale.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * ISO 639 언어 코드 메타데이터.
 *
 * <p>언어 코드의 추가 정보를 저장합니다.</p>
 * <p>참고: ISO 639에는 숫자 코드가 없습니다.</p>
 *
 * @param alpha3 ISO 639-2/T alpha-3 코드 (예: "kor")
 * @param englishName 영문명 (예: "Korean")
 * @param custom 커스텀 추가 항목 여부 (ISO 항목은 null)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LocaleLanguageMetadata(
        String alpha3,
        String englishName,
        Boolean custom
) {

    /**
     * ISO 언어 메타데이터 생성
     */
    public static LocaleLanguageMetadata ofIso(String alpha3, String englishName) {
        return new LocaleLanguageMetadata(alpha3, englishName, null);
    }

    /**
     * 커스텀 언어 메타데이터 생성
     */
    public static LocaleLanguageMetadata ofCustom(String alpha3, String englishName) {
        return new LocaleLanguageMetadata(alpha3, englishName, true);
    }
}
