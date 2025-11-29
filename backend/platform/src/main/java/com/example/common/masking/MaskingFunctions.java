package com.example.common.masking;

import java.util.function.UnaryOperator;

import com.example.common.policy.MaskingMatch;

/**
 * 정책 매치 결과를 문자열 마스킹 함수로 변환하는 헬퍼.
 */
public final class MaskingFunctions {

    private MaskingFunctions() {}

    /**
     * MaskingMatch에 따라 마스킹 함수를 생성합니다.
     * <ul>
     *   <li>매치가 없으면 마스킹 적용 안 함 (원본 반환)</li>
     *   <li>maskingEnabled=false이면 마스킹 해제 (원본 반환)</li>
     *   <li>maskingEnabled=true이면 DataKind의 기본 규칙으로 마스킹</li>
     * </ul>
     *
     * @param match 정책 매칭 결과 (null 가능)
     * @param dataKind 데이터 종류 (마스킹 규칙 결정에 사용)
     * @return 마스킹 함수
     */
    public static UnaryOperator<String> masker(MaskingMatch match, DataKind dataKind) {
        if (match == null) {
            // 매치가 없으면 마스킹 적용 안 함 (원본 반환)
            return UnaryOperator.identity();
        }

        if (!match.isMaskingEnabled()) {
            // 화이트리스트: 마스킹 해제
            return UnaryOperator.identity();
        }

        // 블랙리스트: DataKind 기반 마스킹 적용
        DataKind kind = dataKind != null ? dataKind : DataKind.DEFAULT;
        return value -> applyMaskRule(kind.getDefaultMaskRule(), value);
    }

    /**
     * MaskingMatch에 따라 마스킹 함수를 생성합니다.
     * dataKind가 MaskingMatch에 포함된 경우 사용.
     *
     * @param match 정책 매칭 결과 (null 가능)
     * @return 마스킹 함수
     */
    public static UnaryOperator<String> masker(MaskingMatch match) {
        if (match == null) {
            // 매치가 없으면 마스킹 적용 안 함 (원본 반환)
            return UnaryOperator.identity();
        }

        DataKind kind = DataKind.fromString(match.getDataKind());
        return masker(match, kind);
    }

    private static String applyMaskRule(MaskRule rule, String value) {
        if (value == null || rule == null) {
            return value;
        }
        return switch (rule) {
            case NONE -> value;
            case PARTIAL -> MaskRuleProcessor.apply("PARTIAL", value, null);
            case FULL -> MaskRuleProcessor.apply("FULL", value, null);
            case HASH -> MaskRuleProcessor.apply("HASH", value, null);
            case TOKENIZE -> MaskRuleProcessor.apply("TOKENIZE", value, null);
        };
    }

}
