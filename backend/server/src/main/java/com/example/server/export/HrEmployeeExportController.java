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
import com.example.dw.application.export.ExportExecutionHelper;
import com.example.dw.infrastructure.persistence.HrEmployeeRepository;

/**
 * data-integration 모듈의 HR 데이터를 실제 ExportExecutionHelper/ExportAuditService로 연결한 예시 엔드포인트.
 * 실운영 시 페이징/필터/대량 스트리밍으로 확장 가능.
 */
@RestController
@RequiredArgsConstructor
public class HrEmployeeExportController {

    private final HrEmployeeRepository hrEmployeeRepository;
    private final ExportExecutionHelper exportExecutionHelper;

    @GetMapping("/api/exports/hr-employees")
    public ResponseEntity<byte[]> exportHrEmployees(@RequestParam(name = "limit", defaultValue = "100") int limit,
                                                    @RequestParam(name = "reasonCode") String reasonCode,
                                                    @RequestParam(name = "reasonText", required = false) String reasonText,
                                                    @RequestParam(name = "legalBasisCode", required = false) String legalBasisCode,
                                                    @RequestParam(name = "forceUnmask", required = false, defaultValue = "false") boolean forceUnmask) {
        MaskingTarget target = MaskingContextHolder.get();
        if (target == null) {
            target = MaskingTarget.builder().defaultMask(true).build();
        }
        if (forceUnmask) {
            target = target.toBuilder().forceUnmask(true).build();
        }

        var employees = hrEmployeeRepository.findAll().stream()
                .limit(Math.max(limit, 0))
                .map(e -> Map.<String, Object>of(
                        "employeeId", e.getEmployeeId(),
                        "fullName", e.getFullName(),
                        "organizationCode", e.getOrganizationCode(),
                        "employmentType", e.getEmploymentType(),
                        "employmentStatus", e.getEmploymentStatus(),
                        "email", e.getEmail()))
                .collect(Collectors.toList());

        ExportCommand command = new ExportCommand(
                "csv",
                "hr_employees.csv",
                employees.size(),
                Map.of("source", "hr-employees"),
                reasonCode,
                reasonText,
                legalBasisCode,
                com.example.audit.AuditMode.ASYNC_FALLBACK);

        byte[] body = exportExecutionHelper.exportCsv(
                command,
                employees,
                target,
                "PARTIAL",
                "{\"keepEnd\":4}");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"hr_employees.csv\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(body);
    }
}
