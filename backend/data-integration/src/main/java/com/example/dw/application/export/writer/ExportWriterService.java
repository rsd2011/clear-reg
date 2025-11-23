package com.example.dw.application.export.writer;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.springframework.stereotype.Service;

import com.example.common.masking.MaskingTarget;
import com.example.dw.application.export.ExcelMaskingAdapter;
import com.example.dw.application.export.ExportCommand;
import com.example.dw.application.export.ExportExecutionHelper;
import com.example.dw.application.export.ExportMaskingHelper;
import com.example.dw.application.export.ExportService;
import com.example.dw.application.export.PdfMaskingAdapter;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExportWriterService {

    private final ExportService exportService;
    private final ExportExecutionHelper executionHelper;

    public byte[] exportExcel(ExportCommand command,
                              List<Map<String, Object>> rows,
                              MaskingTarget target,
                              String maskRule,
                              String maskParams,
                              BiConsumer<Integer, Map<String, Object>> writer) {
        return exportService.export(command, () -> {
            for (int i = 0; i < rows.size(); i++) {
                Map<String, Object> masked = ExportMaskingHelper.maskRow(rows.get(i), target, maskRule, maskParams);
                writer.accept(i, masked);
            }
            return new byte[0]; // 실제 Excel 파일은 writer가 처리한다고 가정
        });
    }

    public byte[] exportPdf(ExportCommand command,
                            List<Map<String, Object>> rows,
                            MaskingTarget target,
                            String maskRule,
                            String maskParams,
                            java.util.function.Consumer<String> writer) {
        return exportService.export(command, () -> {
            for (Map<String, Object> row : rows) {
                PdfMaskingAdapter.writeMaskedParagraph(row, target, maskRule, maskParams, writer);
            }
            return new byte[0]; // 실제 PDF writer가 처리한다고 가정
        });
    }

    public byte[] exportCsv(ExportCommand command,
                            List<Map<String, Object>> rows,
                            MaskingTarget target,
                            String maskRule,
                            String maskParams) {
        return executionHelper.exportCsv(command, rows, target, maskRule, maskParams);
    }

    public byte[] exportJson(ExportCommand command,
                             List<Map<String, Object>> rows,
                             MaskingTarget target,
                             String maskRule,
                             String maskParams) {
        return executionHelper.exportJson(command, rows, target, maskRule, maskParams);
    }
}
