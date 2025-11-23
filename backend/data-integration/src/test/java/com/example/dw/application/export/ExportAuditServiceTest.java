package com.example.dw.application.export;

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

class ExportAuditServiceTest {

    AuditPort auditPort = Mockito.mock(AuditPort.class);

    ExportAuditService service = new ExportAuditService(auditPort);

    @Test
    @DisplayName("export 감사 이벤트를 기록한다")
    void recordExportAudit() {
        service.auditExport("excel", 123L, "RSN01", "다운로드 사유", "PIPA", "OK", true, AuditMode.ASYNC_FALLBACK, Map.of("fileName", "customers.xlsx"));

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).record(captor.capture(), Mockito.eq(AuditMode.ASYNC_FALLBACK));
        AuditEvent event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo("EXPORT");
        assertThat(event.getAction()).isEqualTo("EXPORT_EXCEL");
        assertThat(event.getExtra().get("recordCount")).isEqualTo(123L);
        assertThat(event.getExtra().get("fileName")).isEqualTo("customers.xlsx");
        assertThat(event.getReasonCode()).isEqualTo("RSN01");
        assertThat(event.getReasonText()).isEqualTo("다운로드 사유");
        assertThat(event.getLegalBasisCode()).isEqualTo("PIPA");
        assertThat(event.getResultCode()).isEqualTo("OK");
    }
}
