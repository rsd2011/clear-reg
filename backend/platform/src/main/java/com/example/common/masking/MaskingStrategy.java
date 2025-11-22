package com.example.common.masking;

/**
 * 마스킹 여부 및 방식 결정용 전략 인터페이스.
 */
public interface MaskingStrategy {

    boolean shouldMask(MaskingTarget target);

    default String apply(String raw, MaskingTarget target, String maskedValue) {
        return apply(raw, target, maskedValue, null);
    }

    default String apply(String raw, MaskingTarget target, String maskedValue, String fieldName) {
        if (raw == null) return null;
        if (target != null) {
            if (target.isForceUnmask()) {
                return raw;
            }
            if (target.getForceUnmaskKinds() != null && target.getDataKind() != null
                    && target.getForceUnmaskKinds().contains(target.getDataKind())) {
                return raw;
            }
            if (target.getForceUnmaskFields() != null && fieldName != null
                    && target.getForceUnmaskFields().contains(fieldName)) {
                return raw;
            }
        }
        return shouldMask(target) ? maskedValue : raw;
    }
}
