package com.example.common.masking;

import java.util.function.UnaryOperator;

/**
 * 포맷(Excel/PDF/XML/CSV/JSON 등) 무관하게 값에 마스킹을 적용하기 위한 공통 어댑터.
 */
public class OutputMaskingAdapter {

    private OutputMaskingAdapter() {}

    /**
     * 문자열/Maskable 값에 정책 기반 마스킹을 적용한다.
     * @param fieldName 필드명(forceUnmaskFields 매칭용)
     * @param value 문자열 또는 Maskable
     * @param target MaskingTarget (forceUnmask, requesterRoles 등 포함)
     * @param maskRule 정책 maskRule (NONE/PARTIAL/FULL/HASH/TOKENIZE 등)
     * @param maskParams maskRule 파라미터(JSON 등)
     * @return 마스킹된 문자열
     */
    public static String mask(String fieldName, Object value,
                              MaskingTarget target,
                              String maskRule,
                              String maskParams) {
        if (value == null) return null;

        // forceUnmask: 필드 이름/데이터 종류로 해제 요청이 있는 경우 원문 반환
        if (target != null) {
            boolean fieldUnmask = target.getForceUnmaskFields() != null && target.getForceUnmaskFields().contains(fieldName);
            boolean kindUnmask = target.getForceUnmaskKinds() != null && target.getForceUnmaskKinds().contains(target.getDataKind());
            if (target.isForceUnmask() || fieldUnmask || kindUnmask) {
                return value.toString();
            }
        }

        UnaryOperator<String> masker = MaskingFunctions.masker(com.example.common.policy.MaskingMatch.builder()
                .maskRule(maskRule)
                .maskParams(maskParams)
                .build());

        if (value instanceof Maskable maskable) {
            // Maskable은 raw/masked 제공: 정책 기반 masker를 우선 적용
            String processed = masker.apply(maskable.raw());
            return processed;
        }
        return masker.apply(value.toString());
    }
}
