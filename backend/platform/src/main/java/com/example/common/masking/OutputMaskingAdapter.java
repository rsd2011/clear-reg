package com.example.common.masking;

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
     * @param maskingEnabled 마스킹 적용 여부 (false: 화이트리스트로 해제)
     * @return 마스킹된 문자열
     */
    public static String mask(String fieldName, Object value,
                              MaskingTarget target,
                              boolean maskingEnabled) {
        if (value == null) return null;

        // forceUnmask: 필드 이름/데이터 종류로 해제 요청이 있는 경우 원문 반환
        if (target != null) {
            boolean fieldUnmask = target.getForceUnmaskFields() != null && target.getForceUnmaskFields().contains(fieldName);
            boolean kindUnmask = target.getForceUnmaskKinds() != null && target.getForceUnmaskKinds().contains(target.getDataKind());
            if (target.isForceUnmask() || fieldUnmask || kindUnmask) {
                return value.toString();
            }
        }

        // 화이트리스트: 마스킹 해제
        if (!maskingEnabled) {
            if (value instanceof Maskable<?> maskable) {
                Object raw = maskable.raw();
                return raw != null ? raw.toString() : null;
            }
            return value.toString();
        }

        // DataKind 결정: target에 지정된 값 > Maskable의 dataKind > DEFAULT
        DataKind dataKind = resolveDataKind(target, value);

        if (value instanceof Maskable<?> maskable) {
            // Maskable은 DataKind 기반 마스킹 적용
            Object raw = maskable.raw();
            return applyDataKindMasking(raw != null ? raw.toString() : null, dataKind);
        }

        // 일반 문자열은 DataKind 기반 마스킹 적용
        return applyDataKindMasking(value.toString(), dataKind);
    }

    /**
     * DataKind 기반 마스킹을 직접 적용한다.
     * MaskingMatch가 없는 경우에도 DataKind의 기본 규칙에 따라 마스킹을 적용한다.
     */
    private static String applyDataKindMasking(String value, DataKind dataKind) {
        if (value == null) {
            return null;
        }
        MaskRule rule = dataKind != null ? dataKind.getDefaultMaskRule() : MaskRule.FULL;
        return MaskRuleProcessor.apply(rule.name(), value, null);
    }

    /**
     * DataKind를 결정한다.
     * 우선순위: target의 dataKind > Maskable의 dataKind > DEFAULT
     */
    private static DataKind resolveDataKind(MaskingTarget target, Object value) {
        // target에 dataKind가 지정된 경우 우선 사용
        if (target != null && target.getDataKind() != null) {
            return target.getDataKind();
        }
        // Maskable의 dataKind 사용
        if (value instanceof Maskable<?> maskable) {
            DataKind fromMaskable = maskable.dataKind();
            if (fromMaskable != null) {
                return fromMaskable;
            }
        }
        return DataKind.DEFAULT;
    }

    /**
     * 문자열/Maskable 값에 정책 기반 마스킹을 적용한다.
     * 기본적으로 마스킹을 적용한다 (maskingEnabled=true).
     */
    public static String mask(String fieldName, Object value, MaskingTarget target) {
        return mask(fieldName, value, target, true);
    }
}
