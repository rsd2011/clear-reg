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

}

class ExportServiceTestUtil {
    static com.example.dw.application.export.ExportFailureNotifier noopNotifier() {
        return event -> {};
    }
}
