package com.example.common.masking;

import java.util.function.UnaryOperator;

import com.example.common.policy.DataPolicyMatch;

/**
 * 정책 매치 결과를 문자열 마스킹 함수로 변환하는 헬퍼.
 */
public final class MaskingFunctions {

    private MaskingFunctions() {}

    /**
     * DataPolicyMatch의 maskRule/maskParams를 적용하는 UnaryOperator 생성.
     * 매치가 없거나 규칙이 없으면 identity 반환.
     */
    public static UnaryOperator<String> masker(DataPolicyMatch match) {
        if (match == null || match.getMaskRule() == null) {
            return UnaryOperator.identity();
        }
        String rule = match.getMaskRule();
        String params = match.getMaskParams();
        return value -> MaskRuleProcessor.apply(rule, value, params);
    }
}
