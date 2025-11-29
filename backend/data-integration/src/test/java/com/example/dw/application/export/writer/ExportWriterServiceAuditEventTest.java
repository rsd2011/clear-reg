package com.example.dw.application.export.writer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.audit.AuditMode;
import com.example.dw.application.export.ExportAuditService;
import com.example.dw.application.dto.ExportCommand;
import com.example.dw.application.export.ExportExecutionHelper;
import com.example.dw.application.export.ExportFailureNotifier;
import com.example.dw.application.export.ExportService;
import com.example.common.masking.MaskingTarget;

class ExportWriterServiceAuditEventTest {

    @Test
    @DisplayName("Excel/PDF export 시 AuditExport가 호출되고 fileName/recordCount가 기록된다")
    void auditEventsRecorded() {
        ExportAuditService audit = Mockito.mock(ExportAuditService.class);
        ExportFailureNotifier notifier = Mockito.mock(ExportFailureNotifier.class);
        ExportService svc = new ExportService(audit, notifier);
        ExportExecutionHelper helper = new ExportExecutionHelper(svc, new com.fasterxml.jackson.databind.ObjectMapper());
        ExportWriterService writer = new ExportWriterService(svc, helper);

        ExportCommand excelCmd = new ExportCommand("excel", "orgs.xlsx", 2, Map.of(), "R", null, null, AuditMode.ASYNC_FALLBACK);
        ExportCommand pdfCmd = new ExportCommand("pdf", "orgs.pdf", 2, Map.of(), "R", null, null, AuditMode.ASYNC_FALLBACK);
        List<Map<String, Object>> rows = List.of(Map.of("orgCode", "001", "orgName", "HQ"), Map.of("orgCode", "002", "orgName", "BR"));
        MaskingTarget target = MaskingTarget.builder().defaultMask(true).build();

        writer.exportExcel(excelCmd, rows, target, true, (i, r) -> {});
        writer.exportPdf(pdfCmd, rows, target, true, p -> {});

        verify(audit).auditExport("excel", 2, "R", null, null, "OK", true, AuditMode.ASYNC_FALLBACK, Map.of("fileName", "orgs.xlsx"));
        verify(audit).auditExport("pdf", 2, "R", null, null, "OK", true, AuditMode.ASYNC_FALLBACK, Map.of("fileName", "orgs.pdf"));
    }
}
