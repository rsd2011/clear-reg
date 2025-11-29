package com.example.server.export;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import com.example.dw.application.export.writer.ExportWriterService;
import com.example.dw.infrastructure.persistence.HrOrganizationRepository;

/**
 * 조직 대량 Export (Excel/PDF) 예시 엔드포인트.
 * ExportCommand + ExportWriterService 경로로 Audit/마스킹을 일관 적용한다.
 */
@RestController
@Tag(name = "Organization Export", description = "조직 Export API")
@RequiredArgsConstructor
public class OrganizationExportController {

    private final HrOrganizationRepository organizationRepository;
    private final ExportWriterService exportWriterService;

    @SuppressWarnings("unchecked")
    @GetMapping("/api/exports/orgs/excel")
    public ResponseEntity<byte[]> exportOrgsExcel(@RequestParam(name = "limit", defaultValue = "100") int limit,
                                                  @RequestParam String reasonCode,
                                                  @RequestParam(required = false) String reasonText,
                                                  @RequestParam(required = false) String legalBasisCode,
                                                  @RequestParam(defaultValue = "false") boolean forceUnmask) {
        MaskingTarget target = defaultTarget(forceUnmask);

        var rows = organizationRepository.findAll().stream()
                .limit(Math.max(limit, 0))
                .map(o -> Map.<String, Object>of(
                        "orgCode", nvl(o.getOrganizationCode()),
                        "orgName", nvl(o.getName()),
                        "parentCode", nvl(o.getParentOrganizationCode())))
                .collect(Collectors.toList());

        ExportCommand cmd = new ExportCommand("excel", "organizations.xlsx", rows.size(),
                Map.of("source", "org-export"), reasonCode, reasonText, legalBasisCode,
                com.example.audit.AuditMode.ASYNC_FALLBACK);

        byte[] body = exportWriterService.exportExcel(cmd, (List<Map<String, Object>>) (List<?>) rows, target, true, (idx, row) -> {
            // ByteArrayOutputStream은 ExportWriterService 쪽에서 보관하며 row를 줄 단위로 쌓는다.
        });

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"organizations.xlsx\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/api/exports/orgs/pdf")
    public ResponseEntity<byte[]> exportOrgsPdf(@RequestParam(name = "limit", defaultValue = "100") int limit,
                                                @RequestParam String reasonCode,
                                                @RequestParam(required = false) String reasonText,
                                                @RequestParam(required = false) String legalBasisCode,
                                                @RequestParam(defaultValue = "false") boolean forceUnmask) {
        MaskingTarget target = defaultTarget(forceUnmask);

        var rows = organizationRepository.findAll().stream()
                .limit(Math.max(limit, 0))
                .map(o -> Map.<String, Object>of(
                        "orgCode", nvl(o.getOrganizationCode()),
                        "orgName", nvl(o.getName()),
                        "parentCode", nvl(o.getParentOrganizationCode())))
                .collect(Collectors.toList());

        ExportCommand cmd = new ExportCommand("pdf", "organizations.pdf", rows.size(),
                Map.of("source", "org-export"), reasonCode, reasonText, legalBasisCode,
                com.example.audit.AuditMode.ASYNC_FALLBACK);

        byte[] body = exportWriterService.exportPdf(cmd, (List<Map<String, Object>>) (List<?>) rows, target, true, paragraph -> {
            // ExportWriterService에서 ByteArrayOutputStream에 누적된 텍스트를 반환한다.
        });

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"organizations.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(body);
    }

    private MaskingTarget defaultTarget(boolean forceUnmask) {
        MaskingTarget target = MaskingContextHolder.get();
        if (target == null) {
            target = MaskingTarget.builder().defaultMask(true).build();
        }
        if (forceUnmask) {
            target = target.toBuilder().forceUnmask(true).build();
        }
        return target;
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }
}
