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

import lombok.RequiredArgsConstructor;

import com.example.common.masking.MaskingContextHolder;
import com.example.common.masking.MaskingTarget;
import com.example.dw.application.export.ExportCommand;
import com.example.dw.application.export.writer.ExportWriterService;
import com.example.dw.infrastructure.persistence.HrOrganizationRepository;

/**
 * 조직 대량 Export (Excel/PDF) 예시 엔드포인트.
 * ExportCommand + ExportWriterService 경로로 Audit/마스킹을 일관 적용한다.
 */
@RestController
@RequiredArgsConstructor
public class OrganizationExportController {

    private final HrOrganizationRepository organizationRepository;
    private final ExportWriterService exportWriterService;

    @GetMapping("/api/exports/orgs/excel")
    public ResponseEntity<byte[]> exportOrgsExcel(@RequestParam(name = "limit", defaultValue = "100") int limit,
                                                  @RequestParam String reasonCode,
                                                  @RequestParam(required = false) String reasonText,
                                                  @RequestParam(required = false) String legalBasisCode) {
        MaskingTarget target = defaultTarget();

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

        byte[] body = exportWriterService.exportExcel(cmd, (List<Map<String, Object>>) (List<?>) rows, target, "PARTIAL", "{}", (idx, row) -> {
            // 실제 Excel 작성기는 외부 제공 예정. 여기서는 Audit/마스킹 경로만 통과.
        });

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"organizations.xlsx\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }

    @GetMapping("/api/exports/orgs/pdf")
    public ResponseEntity<byte[]> exportOrgsPdf(@RequestParam(name = "limit", defaultValue = "100") int limit,
                                                @RequestParam String reasonCode,
                                                @RequestParam(required = false) String reasonText,
                                                @RequestParam(required = false) String legalBasisCode) {
        MaskingTarget target = defaultTarget();

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

        byte[] body = exportWriterService.exportPdf(cmd, (List<Map<String, Object>>) (List<?>) rows, target, "PARTIAL", "{}", paragraph -> {
            // 실제 PDF 작성기는 외부 제공 예정. Audit/마스킹 경로만 통과.
        });

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"organizations.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(body);
    }

    private MaskingTarget defaultTarget() {
        MaskingTarget target = MaskingContextHolder.get();
        if (target == null) {
            target = MaskingTarget.builder().defaultMask(true).build();
        }
        return target;
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }
}
