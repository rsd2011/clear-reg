package com.example.dw.application.export;

import java.util.Map;
import java.util.function.BiConsumer;

import com.example.common.masking.MaskingTarget;

/**
 * SXSSF 등 엑셀 Writer 앞단에서 마스킹된 값을 셀에 쓰기 위한 어댑터.
 * 실제 셀 쓰기 로직은 caller가 제공하는 consumer(rowIdx, maskedRow)로 위임한다.
 */
public final class ExcelMaskingAdapter {

    private ExcelMaskingAdapter() {}

    public static void writeMaskedRow(int rowIndex,
                                      Map<String, Object> row,
                                      MaskingTarget target,
                                      String maskRule,
                                      String maskParams,
                                      BiConsumer<Integer, Map<String, Object>> rowWriter) {
        Map<String, Object> masked = ExportMaskingHelper.maskRow(row, target, maskRule, maskParams);
        rowWriter.accept(rowIndex, masked);
    }
}
