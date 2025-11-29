package com.example.server.export;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import com.example.common.masking.MaskingContextHolder;
import com.example.common.masking.MaskingTarget;
import com.example.dw.application.dto.ExportCommand;
import com.example.dw.application.export.ExportExecutionHelper;

/**
 * 예시용 export 엔드포인트.
 * 실제 대량 export 구현 시 ExportService와 OutputMaskingAdapter를 통해
 * 감사 로깅 및 정책 기반 마스킹을 일관되게 적용하는 패턴을 보여준다.
 */
@RestController
@Tag(name = "Export", description = "데이터 Export 샘플 API")
@RequiredArgsConstructor
public class ExportController {

    private final ExportExecutionHelper exportExecutionHelper;

    @GetMapping("/api/exports/sample")
    public ResponseEntity<byte[]> sampleCsv(@RequestParam("accountNumber") String accountNumber,
                                            @RequestParam("reasonCode") String reasonCode,
                                            @RequestParam(value = "reasonText", required = false) String reasonText,
                                            @RequestParam(value = "legalBasisCode", required = false) String legalBasisCode,
                                            @RequestParam(value = "forceUnmask", required = false, defaultValue = "false") boolean forceUnmask) {
        MaskingTarget target = MaskingContextHolder.get();
        if (target == null) {
            target = MaskingTarget.builder().defaultMask(true).build();
        }
        if (forceUnmask) {
            target = target.toBuilder().forceUnmask(true).build();
        }

        ExportCommand command = new ExportCommand(
                "csv",
                "sample.csv",
                1,
                Map.of("source", "sample-api"),
                reasonCode,
                reasonText,
                legalBasisCode,
                com.example.audit.AuditMode.ASYNC_FALLBACK);

        byte[] body = exportExecutionHelper.exportCsv(
                command,
                List.of(Map.of("accountNumber", accountNumber)),
                target,
                true);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sample.csv\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(body);
    }

    @GetMapping(value = "/api/exports/sample.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> sampleJson(@RequestParam("accountNumber") String accountNumber,
                                             @RequestParam("reasonCode") String reasonCode,
                                             @RequestParam(value = "forceUnmask", required = false, defaultValue = "false") boolean forceUnmask) {
        MaskingTarget target = MaskingContextHolder.get();
        if (target == null) {
            target = MaskingTarget.builder().defaultMask(true).build();
        }
        if (forceUnmask) {
            target = target.toBuilder().forceUnmask(true).build();
        }

        ExportCommand command = new ExportCommand(
                "json",
                "sample.json",
                1,
                Map.of("source", "sample-api"),
                reasonCode,
                null,
                null,
                com.example.audit.AuditMode.ASYNC_FALLBACK);

        byte[] body = exportExecutionHelper.exportJson(
                command,
                List.of(Map.of("accountNumber", accountNumber)),
                target,
                true);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sample.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}
