package com.example.dw.application.export;

import com.example.dw.application.dto.ExportCommand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPort;

class ExportServiceTest {

    AuditPort auditPort = Mockito.mock(AuditPort.class);
    ExportFailureNotifier notifier = Mockito.mock(ExportFailureNotifier.class);
    ExportAuditService auditService = new ExportAuditService(auditPort);
    ExportService exportService = new ExportService(auditService, notifier);

    @Test
    @DisplayName("export 성공 시 AuditEvent를 남긴다")
    void exportSuccessAudits() {
        ExportCommand cmd = new ExportCommand("csv", "users.csv", 10, Map.of("org", "001"), "RSN01", "고객요청", "PIPA", null);

        String result = exportService.export(cmd, () -> "ok");

        assertThat(result).isEqualTo("ok");
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).record(captor.capture(), Mockito.eq(AuditMode.ASYNC_FALLBACK));
        assertThat(captor.getValue().getExtra().get("fileName")).isEqualTo("users.csv");
        assertThat(captor.getValue().getExtra().get("org")).isEqualTo("001");
        assertThat(captor.getValue().getReasonCode()).isEqualTo("RSN01");
        assertThat(captor.getValue().getReasonText()).isEqualTo("고객요청");
        assertThat(captor.getValue().getLegalBasisCode()).isEqualTo("PIPA");
    }

    @Test
    @DisplayName("export 실패도 AuditEvent로 남기고 예외를 재던진다")
    void exportFailureAuditsAndThrows() {
        ExportCommand cmd = new ExportCommand("excel", "fail.xlsx", 0, Map.of(), null, null, null, AuditMode.STRICT);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> exportService.export(cmd, () -> { throw new IllegalStateException("fail"); }))
                .isInstanceOf(IllegalStateException.class);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).record(captor.capture(), Mockito.eq(AuditMode.STRICT));
        assertThat(captor.getValue().isSuccess()).isFalse();
        assertThat(captor.getValue().getResultCode()).isEqualTo("IllegalStateException");
        ArgumentCaptor<ExportFailureEvent> failureCaptor = ArgumentCaptor.forClass(ExportFailureEvent.class);
        verify(notifier).notify(failureCaptor.capture());
        assertThat(failureCaptor.getValue().getFileName()).isEqualTo("fail.xlsx");
    }

    @Test
    @DisplayName("meta가 null이면 빈 맵으로 처리한다")
    void exportWithNullMeta() {
        ExportCommand cmd = new ExportCommand("csv", "test.csv", 5, null, null, null, null, null);

        String result = exportService.export(cmd, () -> "done");

        assertThat(result).isEqualTo("done");
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).record(captor.capture(), Mockito.eq(AuditMode.ASYNC_FALLBACK));
        assertThat(captor.getValue().getExtra()).containsKey("fileName");
    }

    @Test
    @DisplayName("meta에 이미 fileName이 있으면 덮어쓰지 않는다")
    void exportWithExistingFileNameInMeta() {
        ExportCommand cmd = new ExportCommand("csv", "actual.csv", 5, Map.of("fileName", "original.csv"), null, null, null, null);

        exportService.export(cmd, () -> "ok");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).record(captor.capture(), Mockito.eq(AuditMode.ASYNC_FALLBACK));
        assertThat(captor.getValue().getExtra().get("fileName")).isEqualTo("original.csv");
    }

    @Test
    @DisplayName("fileName이 null이면 meta에 추가하지 않는다")
    void exportWithNullFileName() {
        ExportCommand cmd = new ExportCommand("csv", null, 5, Map.of("key", "value"), null, null, null, null);

        exportService.export(cmd, () -> "ok");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).record(captor.capture(), Mockito.eq(AuditMode.ASYNC_FALLBACK));
        assertThat(captor.getValue().getExtra()).containsKey("key");
        assertThat(captor.getValue().getExtra()).doesNotContainKey("fileName");
    }
}
