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
import com.example.common.masking.MaskingTarget;
import com.example.dw.application.export.ExportCommand;
import com.example.dw.application.export.ExportExecutionHelper;
import com.example.dw.application.export.ExportService;
import com.example.dw.application.export.PdfMaskingAdapter;

class ExportWriterServiceTest {

    AuditPort auditPort = Mockito.mock(AuditPort.class);
    ExportService exportService = new ExportService(new com.example.dw.application.export.ExportAuditService(auditPort), ExportServiceTestUtil.noopNotifier());
    ExportExecutionHelper helper = Mockito.mock(ExportExecutionHelper.class);
    ExportWriterService service = new ExportWriterService(exportService, helper);

    @Test
    @DisplayName("Excel writer에 마스킹된 행이 전달된다")
    void excelWriterMasked() {
        ExportCommand cmd = new ExportCommand("excel", "x.xlsx", 1, Map.of(), null, null, null, AuditMode.ASYNC_FALLBACK);
        MaskingTarget target = MaskingTarget.builder().dataKind("accountNumber").build();
        AtomicInteger counter = new AtomicInteger();
        byte[] result = service.exportExcel(cmd, List.of(Map.of("account", "1234-5678-9012")), target, "PARTIAL", null,
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
        MaskingTarget target = MaskingTarget.builder().dataKind("rrn").build();
        java.util.function.Consumer<String> writer = Mockito.mock(java.util.function.Consumer.class);

        service.exportPdf(cmd, List.of(Map.of("rrn", "900101-1234567")), target, "PARTIAL", null, writer);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(writer).accept(captor.capture());
        assertThat(captor.getValue()).doesNotContain("900101-1234567");
    }

    @Test
    @DisplayName("CSV/JSON 경로는 ExecutionHelper를 그대로 호출")
    void delegatesCsvJson() {
        ExportCommand cmd = new ExportCommand("csv", "x.csv", 1, Map.of(), null, null, null, AuditMode.ASYNC_FALLBACK);
        MaskingTarget target = MaskingTarget.builder().build();
        when(helper.exportCsv(Mockito.eq(cmd), Mockito.anyList(), Mockito.eq(target), Mockito.eq("PARTIAL"), Mockito.isNull()))
                .thenReturn(new byte[]{1,2});
        byte[] body = service.exportCsv(cmd, List.of(Map.of()), target, "PARTIAL", null);
        assertThat(body).containsExactly(1,2);
    }
}

class ExportServiceTestUtil {
    static com.example.dw.application.export.ExportFailureNotifier noopNotifier() {
        return event -> {};
    }
}
