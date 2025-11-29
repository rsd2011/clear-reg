package com.example.common.masking;

import java.util.function.UnaryOperator;

import com.example.common.policy.DataPolicyMatch;
import com.example.common.policy.MaskingMatch;

/**
 * 정책 매치 결과를 문자열 마스킹 함수로 변환하는 헬퍼.
 */
public final class MaskingFunctions {

    private MaskingFunctions() {}

    /**
     * MaskingMatch의 maskRule/maskParams를 적용하는 UnaryOperator 생성.
     * 매치가 없거나 규칙이 없으면 identity 반환.
     */
    public static UnaryOperator<String> masker(MaskingMatch match) {
        if (match == null || match.getMaskRule() == null) {
            return UnaryOperator.identity();
        }
        String rule = match.getMaskRule();
        String params = match.getMaskParams();
        return value -> MaskRuleProcessor.apply(rule, value, params);
    }

    /**
     * DataPolicyMatch의 maskRule/maskParams를 적용하는 UnaryOperator 생성.
     * 매치가 없거나 규칙이 없으면 identity 반환.
     * @deprecated MaskingMatch를 사용하는 버전으로 마이그레이션하세요.
     */
    @Deprecated(forRemoval = true)
    public static UnaryOperator<String> masker(DataPolicyMatch match) {
        if (match == null || match.getMaskRule() == null) {
            return UnaryOperator.identity();
        }
        String rule = match.getMaskRule();
        String params = match.getMaskParams();
        return value -> MaskRuleProcessor.apply(rule, value, params);
    }
}
