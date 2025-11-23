package com.example.dw.application.export;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.common.masking.MaskingTarget;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import lombok.RequiredArgsConstructor;

/**
 * 대량 export 호출부에서 OutputMaskingAdapter와 ExportService를 일관되게 사용하도록 돕는 헬퍼.
 * 실제 Writer 구현(Excel/PDF/CSV/JSON 등)은 외부 라이브러리와 연동해도 되고,
 * 공통 패턴은 여기서 제공한다.
 */
@Component
@RequiredArgsConstructor
public class ExportExecutionHelper {

    private final ExportService exportService;
    private final ObjectMapper objectMapper;

    public byte[] exportCsv(ExportCommand command,
                            List<Map<String, Object>> rows,
                            MaskingTarget target,
                            String maskRule,
                            String maskParams) {
        return exportService.export(command, () -> buildCsv(rows, target, maskRule, maskParams));
    }

    public byte[] exportJson(ExportCommand command,
                             List<Map<String, Object>> rows,
                             MaskingTarget target,
                             String maskRule,
                             String maskParams) {
        return exportService.export(command, () -> buildJson(rows, target, maskRule, maskParams));
    }

    private byte[] buildCsv(List<Map<String, Object>> rows,
                            MaskingTarget target,
                            String maskRule,
                            String maskParams) {
        if (rows.isEmpty()) {
            return new byte[0];
        }
        var headers = rows.getFirst().keySet().stream().toList();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String headerLine = String.join(",", headers);
        out.write(headerLine.getBytes(StandardCharsets.UTF_8), 0, headerLine.length());
        out.write('\n');
        for (Map<String, Object> row : rows) {
            Map<String, Object> masked = ExportMaskingHelper.maskRow(row, target, maskRule, maskParams);
            String line = headers.stream()
                    .map(h -> String.valueOf(masked.getOrDefault(h, "")))
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");
            out.write(line.getBytes(StandardCharsets.UTF_8), 0, line.length());
            out.write('\n');
        }
        return out.toByteArray();
    }

    private byte[] buildJson(List<Map<String, Object>> rows,
                             MaskingTarget target,
                             String maskRule,
                             String maskParams) {
        try {
            var masked = rows.stream()
                    .map(r -> ExportMaskingHelper.maskRow(r, target, maskRule, maskParams))
                    .toList();
            ObjectMapper mapper = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
            return mapper.writeValueAsBytes(masked);
        } catch (Exception ex) {
            throw new IllegalStateException("JSON export failed", ex);
        }
    }
}
