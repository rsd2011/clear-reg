package com.example.dw.application.export;

import java.util.Map;
import java.util.function.Consumer;

import com.example.common.masking.MaskingTarget;

/**
 * PDF 텍스트 삽입 전에 필드값을 마스킹하기 위한 어댑터.
 * 실제 PDF 라이브러리는 외부에서 주입하며, 여기서는 마스킹된 문자열을 consumer로 전달한다.
 */
public final class PdfMaskingAdapter {

    private PdfMaskingAdapter() {}

    public static void writeMaskedParagraph(Map<String, Object> row,
                                            MaskingTarget target,
                                            String maskRule,
                                            String maskParams,
                                            Consumer<String> paragraphWriter) {
        Map<String, Object> masked = ExportMaskingHelper.maskRow(row, target, maskRule, maskParams);
        paragraphWriter.accept(masked.toString());
    }
}
