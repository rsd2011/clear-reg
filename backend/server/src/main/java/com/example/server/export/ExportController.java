package com.example.server.export;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import com.example.common.masking.MaskingContextHolder;
import com.example.common.masking.MaskingTarget;
import com.example.common.masking.OutputMaskingAdapter;
import com.example.dw.application.export.ExportCommand;
import com.example.dw.application.export.ExportService;

/**
 * 예시용 export 엔드포인트.
 * 실제 대량 export 구현 시 ExportService와 OutputMaskingAdapter를 통해
 * 감사 로깅 및 정책 기반 마스킹을 일관되게 적용하는 패턴을 보여준다.
 */
@RestController
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @GetMapping("/api/exports/sample")
    public ResponseEntity<byte[]> sampleCsv(@RequestParam("accountNumber") String accountNumber) {
        MaskingTarget target = MaskingContextHolder.get();
        String masked = OutputMaskingAdapter.mask("accountNumber", accountNumber, target, "PARTIAL", "{\"keepEnd\":4}");

        ExportCommand command = new ExportCommand(
                "csv",
                "sample.csv",
                1,
                Map.of("source", "sample-api"));

        byte[] body = exportService.export(command, () ->
                ("accountNumber\n" + masked + "\n").getBytes(StandardCharsets.UTF_8));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sample.csv\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(body);
    }
}
