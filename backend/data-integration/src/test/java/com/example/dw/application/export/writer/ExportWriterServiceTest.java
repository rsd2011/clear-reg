package com.example.dw.application.export.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.example.audit.AuditMode;
import com.example.audit.AuditPort;
import com.example.common.masking.DataKind;
import com.example.common.masking.MaskingTarget;
import com.example.dw.application.dto.ExportCommand;
import com.example.dw.application.export.ExportExecutionHelper;
import com.example.dw.application.export.ExportService;
import com.example.dw.application.export.PdfMaskingAdapter;

@SuppressWarnings("unchecked")
class ExportWriterServiceTest {

    AuditPort auditPort = Mockito.mock(AuditPort.class);
    ExportService exportService = new ExportService(new com.example.dw.application.export.ExportAuditService(auditPort), ExportServiceTestUtil.noopNotifier());
    ExportExecutionHelper helper = Mockito.mock(ExportExecutionHelper.class);
    ExportWriterService service = new ExportWriterService(exportService, helper);

    @Test
    @DisplayName("Excel writer에 마스킹된 행이 전달된다")
    void excelWriterMasked() {
        ExportCommand cmd = new ExportCommand("excel", "x.xlsx", 1, Map.of(), null, null, null, AuditMode.ASYNC_FALLBACK);
        MaskingTarget target = MaskingTarget.builder().dataKind(DataKind.ACCOUNT_NO).build();
        AtomicInteger counter = new AtomicInteger();
        byte[] result = service.exportExcel(cmd, List.of(Map.of("account", "1234-5678-9012")), target, true,
                (idx, row) -> {
                    counter.incrementAndGet();
                    org.assertj.core.api.Assertions.assertThat(row.get("account")).isNotEqualTo("1234-5678-9012");
                });
        assertThat(result).isNotEmpty();
        verify(auditPort).record(Mockito.any(), Mockito.eq(AuditMode.ASYNC_FALLBACK));
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("PDF writer에도 마스킹된 필드가 전달된다")
    void pdfWriterMasked() {
        ExportCommand cmd = new ExportCommand("pdf", "x.pdf", 1, Map.of(), null, null, null, AuditMode.ASYNC_FALLBACK);
        MaskingTarget target = MaskingTarget.builder().dataKind(DataKind.SSN).build();
        java.util.function.Consumer<String> writer = Mockito.mock(java.util.function.Consumer.class);

        service.exportPdf(cmd, List.of(Map.of("rrn", "900101-1234567")), target, true, writer);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(writer).accept(captor.capture());
        assertThat(captor.getValue()).doesNotContain("900101-1234567");
    }

    @Test
    @DisplayName("CSV/JSON 경로는 ExecutionHelper를 그대로 호출")
    void delegatesCsvJson() {
        ExportCommand cmd = new ExportCommand("csv", "x.csv", 1, Map.of(), null, null, null, AuditMode.ASYNC_FALLBACK);
        MaskingTarget target = MaskingTarget.builder().build();
        when(helper.exportCsv(Mockito.eq(cmd), Mockito.anyList(), Mockito.eq(target), Mockito.eq(true)))
                .thenReturn(new byte[]{1,2});
        byte[] body = service.exportCsv(cmd, List.of(Map.of()), target, true);
        assertThat(body).containsExactly(1,2);
    }

    @Test
    @DisplayName("JSON 경로도 ExecutionHelper를 그대로 호출")
    void delegatesJson() {
        ExportCommand cmd = new ExportCommand("json", "x.json", 1, Map.of(), null, null, null, AuditMode.ASYNC_FALLBACK);
        MaskingTarget target = MaskingTarget.builder().build();
        when(helper.exportJson(Mockito.eq(cmd), Mockito.anyList(), Mockito.eq(target), Mockito.eq(true)))
                .thenReturn(new byte[]{3,4});
        byte[] body = service.exportJson(cmd, List.of(Map.of()), target, true);
        assertThat(body).containsExactly(3,4);
    }

    @Test
    @DisplayName("deprecated CSV 메서드는 maskRule에 따라 maskingEnabled를 결정한다")
    void deprecatedCsvMethod() {
        ExportCommand cmd = new ExportCommand("csv", "x.csv", 1, Map.of(), null, null, null, AuditMode.ASYNC_FALLBACK);
        MaskingTarget target = MaskingTarget.builder().build();
        // maskRule=null이면 maskingEnabled=false
        when(helper.exportCsv(Mockito.eq(cmd), Mockito.anyList(), Mockito.eq(target), Mockito.eq(false)))
                .thenReturn(new byte[]{5,6});
        byte[] body = service.exportCsv(cmd, List.of(Map.of()), target, null, null);
        assertThat(body).containsExactly(5,6);
    }

    @Test
    @DisplayName("deprecated JSON 메서드는 maskRule에 따라 maskingEnabled를 결정한다")
    void deprecatedJsonMethod() {
        ExportCommand cmd = new ExportCommand("json", "x.json", 1, Map.of(), null, null, null, AuditMode.ASYNC_FALLBACK);
        MaskingTarget target = MaskingTarget.builder().build();
        // maskRule=PARTIAL이면 maskingEnabled=true
        when(helper.exportJson(Mockito.eq(cmd), Mockito.anyList(), Mockito.eq(target), Mockito.eq(true)))
                .thenReturn(new byte[]{7,8});
        byte[] body = service.exportJson(cmd, List.of(Map.of()), target, "PARTIAL", null);
        assertThat(body).containsExactly(7,8);
    }

    @Test
    @DisplayName("deprecated Excel 메서드는 maskRule에 따라 maskingEnabled를 결정한다")
    void deprecatedExcelMethod() {
        ExportCommand cmd = new ExportCommand("excel", "x.xlsx", 1, Map.of(), null, null, null, AuditMode.ASYNC_FALLBACK);
        MaskingTarget target = MaskingTarget.builder().dataKind(DataKind.SSN).build();
        AtomicInteger counter = new AtomicInteger();
        // maskRule=FULL이면 maskingEnabled=true
        byte[] result = service.exportExcel(cmd, List.of(Map.of("ssn", "123456789")), target, "FULL", null,
                (idx, row) -> counter.incrementAndGet());
        assertThat(result).isNotEmpty();
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("deprecated PDF 메서드는 maskRule에 따라 maskingEnabled를 결정한다")
    void deprecatedPdfMethod() {
        ExportCommand cmd = new ExportCommand("pdf", "x.pdf", 1, Map.of(), null, null, null, AuditMode.ASYNC_FALLBACK);
        MaskingTarget target = MaskingTarget.builder().dataKind(DataKind.SSN).build();
        java.util.function.Consumer<String> writer = Mockito.mock(java.util.function.Consumer.class);
        // maskRule=NONE이면 maskingEnabled=false
        service.exportPdf(cmd, List.of(Map.of("data", "value")), target, "NONE", null, writer);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(writer).accept(captor.capture());
        // NONE → 마스킹 비활성화 → 원본 반환
        assertThat(captor.getValue()).contains("value");
    }

    @Test
    @DisplayName("maskingEnabled=false면 Excel에서 마스킹하지 않는다")
    void excelWriterUnmasked() {
        ExportCommand cmd = new ExportCommand("excel", "x.xlsx", 1, Map.of(), null, null, null, AuditMode.ASYNC_FALLBACK);
        MaskingTarget target = MaskingTarget.builder().dataKind(DataKind.ACCOUNT_NO).build();
        AtomicInteger counter = new AtomicInteger();
        byte[] result = service.exportExcel(cmd, List.of(Map.of("account", "1234-5678-9012")), target, false,
                (idx, row) -> {
                    counter.incrementAndGet();
                    // 마스킹 비활성화 → 원본 유지
                    org.assertj.core.api.Assertions.assertThat(row.get("account")).isEqualTo("1234-5678-9012");
                });
        assertThat(result).isNotEmpty();
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("maskingEnabled=false면 PDF에서 마스킹하지 않는다")
    void pdfWriterUnmasked() {
        ExportCommand cmd = new ExportCommand("pdf", "x.pdf", 1, Map.of(), null, null, null, AuditMode.ASYNC_FALLBACK);
        MaskingTarget target = MaskingTarget.builder().dataKind(DataKind.SSN).build();
        java.util.function.Consumer<String> writer = Mockito.mock(java.util.function.Consumer.class);
        service.exportPdf(cmd, List.of(Map.of("rrn", "900101-1234567")), target, false, writer);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(writer).accept(captor.capture());
        // 마스킹 비활성화 → 원본 유지
        assertThat(captor.getValue()).contains("900101-1234567");
    }

    // ============ 브랜치 커버리지를 위한 추가 테스트 ============

    @Test
    @DisplayName("deprecated CSV: maskRule=NONE이면 maskingEnabled=false")
    void deprecatedCsvNone() {
        ExportCommand cmd = new ExportCommand("csv", "x.csv", 1, Map.of(), null, null, null, AuditMode.ASYNC_FALLBACK);
        MaskingTarget target = MaskingTarget.builder().build();
        when(helper.exportCsv(Mockito.eq(cmd), Mockito.anyList(), Mockito.eq(target), Mockito.eq(false)))
                .thenReturn(new byte[]{10,11});
        byte[] body = service.exportCsv(cmd, List.of(Map.of()), target, "NONE", null);
        assertThat(body).containsExactly(10,11);
    }

    @Test
    @DisplayName("deprecated CSV: maskRule=FULL이면 maskingEnabled=true")
    void deprecatedCsvFull() {
        ExportCommand cmd = new ExportCommand("csv", "x.csv", 1, Map.of(), null, null, null, AuditMode.ASYNC_FALLBACK);
        MaskingTarget target = MaskingTarget.builder().build();
        when(helper.exportCsv(Mockito.eq(cmd), Mockito.anyList(), Mockito.eq(target), Mockito.eq(true)))
                .thenReturn(new byte[]{12,13});
        byte[] body = service.exportCsv(cmd, List.of(Map.of()), target, "FULL", null);
        assertThat(body).containsExactly(12,13);
    }

    @Test
    @DisplayName("deprecated JSON: maskRule=null이면 maskingEnabled=false")
    void deprecatedJsonNull() {
        ExportCommand cmd = new ExportCommand("json", "x.json", 1, Map.of(), null, null, null, AuditMode.ASYNC_FALLBACK);
        MaskingTarget target = MaskingTarget.builder().build();
        when(helper.exportJson(Mockito.eq(cmd), Mockito.anyList(), Mockito.eq(target), Mockito.eq(false)))
                .thenReturn(new byte[]{14,15});
        byte[] body = service.exportJson(cmd, List.of(Map.of()), target, null, null);
        assertThat(body).containsExactly(14,15);
    }

    @Test
    @DisplayName("deprecated JSON: maskRule=NONE이면 maskingEnabled=false")
    void deprecatedJsonNone() {
        ExportCommand cmd = new ExportCommand("json", "x.json", 1, Map.of(), null, null, null, AuditMode.ASYNC_FALLBACK);
        MaskingTarget target = MaskingTarget.builder().build();
        when(helper.exportJson(Mockito.eq(cmd), Mockito.anyList(), Mockito.eq(target), Mockito.eq(false)))
                .thenReturn(new byte[]{16,17});
        byte[] body = service.exportJson(cmd, List.of(Map.of()), target, "NONE", null);
        assertThat(body).containsExactly(16,17);
    }

    @Test
    @DisplayName("deprecated Excel: maskRule=null이면 maskingEnabled=false")
    void deprecatedExcelNull() {
        ExportCommand cmd = new ExportCommand("excel", "x.xlsx", 1, Map.of(), null, null, null, AuditMode.ASYNC_FALLBACK);
        MaskingTarget target = MaskingTarget.builder().dataKind(DataKind.ACCOUNT_NO).build();
        AtomicInteger counter = new AtomicInteger();
        byte[] result = service.exportExcel(cmd, List.of(Map.of("account", "1234-5678-9012")), target, null, null,
                (idx, row) -> {
                    counter.incrementAndGet();
                    // null → maskingEnabled=false → 원본 유지
                    org.assertj.core.api.Assertions.assertThat(row.get("account")).isEqualTo("1234-5678-9012");
                });
        assertThat(result).isNotEmpty();
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("deprecated Excel: maskRule=NONE이면 maskingEnabled=false")
    void deprecatedExcelNone() {
        ExportCommand cmd = new ExportCommand("excel", "x.xlsx", 1, Map.of(), null, null, null, AuditMode.ASYNC_FALLBACK);
        MaskingTarget target = MaskingTarget.builder().dataKind(DataKind.ACCOUNT_NO).build();
        AtomicInteger counter = new AtomicInteger();
        byte[] result = service.exportExcel(cmd, List.of(Map.of("account", "1234-5678-9012")), target, "NONE", null,
                (idx, row) -> {
                    counter.incrementAndGet();
                    // NONE → maskingEnabled=false → 원본 유지
                    org.assertj.core.api.Assertions.assertThat(row.get("account")).isEqualTo("1234-5678-9012");
                });
        assertThat(result).isNotEmpty();
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("deprecated PDF: maskRule=null이면 maskingEnabled=false")
    void deprecatedPdfNull() {
        ExportCommand cmd = new ExportCommand("pdf", "x.pdf", 1, Map.of(), null, null, null, AuditMode.ASYNC_FALLBACK);
        MaskingTarget target = MaskingTarget.builder().dataKind(DataKind.SSN).build();
        java.util.function.Consumer<String> writer = Mockito.mock(java.util.function.Consumer.class);
        service.exportPdf(cmd, List.of(Map.of("data", "value")), target, null, null, writer);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(writer).accept(captor.capture());
        // null → maskingEnabled=false → 원본 반환
        assertThat(captor.getValue()).contains("value");
    }

    @Test
    @DisplayName("deprecated PDF: maskRule=FULL이면 maskingEnabled=true")
    void deprecatedPdfFull() {
        ExportCommand cmd = new ExportCommand("pdf", "x.pdf", 1, Map.of(), null, null, null, AuditMode.ASYNC_FALLBACK);
        MaskingTarget target = MaskingTarget.builder().dataKind(DataKind.SSN).build();
        java.util.function.Consumer<String> writer = Mockito.mock(java.util.function.Consumer.class);
        service.exportPdf(cmd, List.of(Map.of("rrn", "900101-1234567")), target, "FULL", null, writer);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(writer).accept(captor.capture());
        // FULL → maskingEnabled=true → 마스킹 적용
        assertThat(captor.getValue()).doesNotContain("900101-1234567");
    }
}

class ExportServiceTestUtil {
    static com.example.dw.application.export.ExportFailureNotifier noopNotifier() {
        return event -> {};
    }
}
