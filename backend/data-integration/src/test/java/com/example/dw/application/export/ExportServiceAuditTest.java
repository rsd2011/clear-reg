package com.example.dw.application.export;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.audit.AuditMode;

class ExportServiceAuditTest {

    @Test
    @DisplayName("성공 시 AuditExport가 한 번 호출된다")
    void auditsOnSuccess() {
        ExportAuditService audit = Mockito.mock(ExportAuditService.class);
        ExportFailureNotifier notifier = Mockito.mock(ExportFailureNotifier.class);
        ExportService svc = new ExportService(audit, notifier);

        ExportCommand cmd = new ExportCommand("csv", "ok.csv", 1, Map.of(), "R", null, null, AuditMode.ASYNC_FALLBACK);
        svc.export(cmd, () -> "ok");

        verify(audit).auditExport("csv", 1, "R", null, null, "OK", true, AuditMode.ASYNC_FALLBACK, Map.of("fileName", "ok.csv"));
    }

    @Test
    @DisplayName("실패 시 AuditExport 기록 및 Notifier 호출 후 예외를 전파한다")
    void auditsOnFailure() {
        ExportAuditService audit = Mockito.mock(ExportAuditService.class);
        ExportFailureNotifier notifier = Mockito.mock(ExportFailureNotifier.class);
        ExportService svc = new ExportService(audit, notifier);

        ExportCommand cmd = new ExportCommand("csv", "fail.csv", 1, Map.of(), "R", null, null, AuditMode.ASYNC_FALLBACK);

        assertThatThrownBy(() -> svc.export(cmd, () -> { throw new IllegalStateException("boom"); }))
                .isInstanceOf(IllegalStateException.class);

        verify(audit).auditExport("csv", 1, "R", null, null, "IllegalStateException", false, AuditMode.ASYNC_FALLBACK, Map.of("fileName", "fail.csv"));
        verify(notifier).notify(Mockito.any());
    }

    @Test
    @DisplayName("대량 데이터(예: 10k rows)도 메모리 예외 없이 처리한다")
    void handlesLargeDataset() {
        ExportAuditService audit = Mockito.mock(ExportAuditService.class);
        ExportFailureNotifier notifier = Mockito.mock(ExportFailureNotifier.class);
        ExportService svc = new ExportService(audit, notifier);

        ExportCommand cmd = new ExportCommand("csv", "large.csv", 10_000, Map.of(), "R", null, null, AuditMode.ASYNC_FALLBACK);
        String result = svc.export(cmd, () -> {
            // 스트리밍 시뮬레이션: 많은 문자열을 생성하지만 OOM 없이 완료
            StringBuilder sb = new StringBuilder();
            IntStream.range(0, 10_000).forEach(i -> sb.append("row").append(i).append('\n'));
            return sb.toString();
        });

        verify(audit).auditExport("csv", 10_000, "R", null, null, "OK", true, AuditMode.ASYNC_FALLBACK, Map.of("fileName", "large.csv"));
        org.assertj.core.api.Assertions.assertThat(result).contains("row9999");
    }
}
