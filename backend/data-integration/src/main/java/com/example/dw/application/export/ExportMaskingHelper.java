package com.example.dw.application.export;

import java.util.HashMap;
import java.util.Map;

import com.example.common.masking.MaskingTarget;
import com.example.common.masking.OutputMaskingAdapter;

/**
 * Export 시 포맷(writer) 앞단에서 공통 마스킹을 적용하는 헬퍼.
 * Excel/CSV/PDF/JSON 등에서 필드별 마스킹을 일관되게 수행하기 위해 사용한다.
 */
public final class ExportMaskingHelper {

    private ExportMaskingHelper() {}

    /**
     * 주어진 row(Map)의 각 값을 OutputMaskingAdapter를 통해 마스킹한 새 Map을 반환한다.
     *
     * @param row 원본 row 데이터
     * @param target 마스킹 타겟 정보
     * @param maskingEnabled 마스킹 적용 여부 (false: 화이트리스트로 해제)
     * @return 마스킹된 row
     */
    public static Map<String, Object> maskRow(Map<String, Object> row,
                                              MaskingTarget target,
                                              boolean maskingEnabled) {
        Map<String, Object> masked = new HashMap<>();
        if (row == null) {
            return masked;
        }
        row.forEach((k, v) -> masked.put(k, OutputMaskingAdapter.mask(k, v, target, maskingEnabled)));
        return masked;
    }

    /**
     * 주어진 row(Map)의 각 값을 OutputMaskingAdapter를 통해 마스킹한 새 Map을 반환한다.
     * 기본적으로 마스킹을 적용한다 (maskingEnabled=true).
     */
    public static Map<String, Object> maskRow(Map<String, Object> row, MaskingTarget target) {
        return maskRow(row, target, true);
    }

    /**
     * @deprecated maskRule, maskParams 파라미터는 더 이상 사용되지 않습니다.
     *             maskRow(row, target, maskingEnabled)를 사용하세요.
     */
    @Deprecated
    public static Map<String, Object> maskRow(Map<String, Object> row,
                                              MaskingTarget target,
                                              String maskRule,
                                              String maskParams) {
        boolean maskingEnabled = maskRule != null && !"NONE".equalsIgnoreCase(maskRule);
        return maskRow(row, target, maskingEnabled);
    }
}
