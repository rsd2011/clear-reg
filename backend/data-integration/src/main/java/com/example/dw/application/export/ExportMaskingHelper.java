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
     */
    public static Map<String, Object> maskRow(Map<String, Object> row,
                                              MaskingTarget target,
                                              String maskRule,
                                              String maskParams) {
        Map<String, Object> masked = new HashMap<>();
        if (row == null) {
            return masked;
        }
        row.forEach((k, v) -> masked.put(k, OutputMaskingAdapter.mask(k, v, target, maskRule, maskParams)));
        return masked;
    }
}
