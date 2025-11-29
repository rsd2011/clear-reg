package com.example.common.masking;

/**
 * 마스킹 규칙 종류.
 *
 * <p>각 규칙은 데이터를 어떻게 마스킹할지 정의한다.
 */
public enum MaskRule {

    /** 마스킹 없음 (원본 노출) */
    NONE,

    /** 부분 마스킹 (앞뒤 일부만 노출) */
    PARTIAL,

    /** 전체 마스킹 */
    FULL,

    /** 해시 변환 */
    HASH,

    /** 토큰화 */
    TOKENIZE;

    /**
     * 문자열로부터 MaskRule을 찾는다.
     * 대소문자를 무시하며, 찾지 못하면 FULL을 반환한다.
     *
     * @param value 마스킹 규칙 문자열
     * @return 해당하는 MaskRule, 없으면 FULL
     */
    public static MaskRule fromString(String value) {
        if (value == null || value.isBlank()) {
            return FULL;
        }
        try {
            return MaskRule.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return FULL;
        }
    }

    /**
     * 문자열로부터 MaskRule을 찾는다. (fromString과 동일)
     * 찾지 못하면 NONE을 반환한다.
     *
     * @param value 마스킹 규칙 문자열
     * @return 해당하는 MaskRule, 없으면 NONE
     */
    public static MaskRule of(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        try {
            return MaskRule.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
