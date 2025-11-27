package com.example.dw.application.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPort;

@DisplayName("ExportAuditService 테스트")
class ExportAuditServiceTest {

    AuditPort auditPort;
    ExportAuditService service;

    @BeforeEach
    void setUp() {
        auditPort = Mockito.mock(AuditPort.class);
        service = new ExportAuditService(auditPort);
    }

    @Nested
    @DisplayName("auditExport 메서드")
    class AuditExport {

        @Test
        @DisplayName("Given 모든 필드 When 호출하면 Then 완전한 감사 이벤트를 기록한다")
        void recordExportAuditWithAllFields() {
            service.auditExport("excel", 123L, "RSN01", "다운로드 사유", "PIPA", "OK", true, AuditMode.ASYNC_FALLBACK, Map.of("fileName", "customers.xlsx"));

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditPort).record(captor.capture(), eq(AuditMode.ASYNC_FALLBACK));
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

        @Test
        @DisplayName("Given null reasonCode When 호출하면 Then reasonCode 없이 기록한다")
        void recordWithoutReasonCode() {
            service.auditExport("csv", 100L, null, "사유", "PIPA", "OK", true, AuditMode.STRICT, null);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditPort).record(captor.capture(), eq(AuditMode.STRICT));
            AuditEvent event = captor.getValue();
            assertThat(event.getReasonCode()).isNull();
            assertThat(event.getExtra().containsKey("reasonCode")).isFalse();
        }

        @Test
        @DisplayName("Given null reasonText When 호출하면 Then reasonText 없이 기록한다")
        void recordWithoutReasonText() {
            service.auditExport("csv", 100L, "RSN", null, "PIPA", "OK", true, AuditMode.STRICT, null);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditPort).record(captor.capture(), eq(AuditMode.STRICT));
            AuditEvent event = captor.getValue();
            assertThat(event.getReasonText()).isNull();
            assertThat(event.getExtra().containsKey("reasonText")).isFalse();
        }

        @Test
        @DisplayName("Given null legalBasisCode When 호출하면 Then legalBasisCode 없이 기록한다")
        void recordWithoutLegalBasisCode() {
            service.auditExport("csv", 100L, "RSN", "사유", null, "OK", true, AuditMode.STRICT, null);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditPort).record(captor.capture(), eq(AuditMode.STRICT));
            AuditEvent event = captor.getValue();
            assertThat(event.getLegalBasisCode()).isNull();
            assertThat(event.getExtra().containsKey("legalBasisCode")).isFalse();
        }

        @Test
        @DisplayName("Given null resultCode + success=true When 호출하면 Then resultCode=OK로 설정")
        void nullResultCodeWithSuccessSetsOK() {
            service.auditExport("pdf", 50L, null, null, null, null, true, AuditMode.STRICT, null);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditPort).record(captor.capture(), any());
            AuditEvent event = captor.getValue();
            assertThat(event.getResultCode()).isEqualTo("OK");
        }

        @Test
        @DisplayName("Given null resultCode + success=false When 호출하면 Then resultCode=FAIL로 설정")
        void nullResultCodeWithFailureSetsFAIL() {
            service.auditExport("pdf", 0L, null, null, null, null, false, AuditMode.STRICT, null);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditPort).record(captor.capture(), any());
            AuditEvent event = captor.getValue();
            assertThat(event.getResultCode()).isEqualTo("FAIL");
        }

        @Test
        @DisplayName("Given null mode When 호출하면 Then ASYNC_FALLBACK 모드 사용")
        void nullModeUsesAsyncFallback() {
            service.auditExport("json", 10L, null, null, null, "DONE", true, null, null);

            verify(auditPort).record(any(), eq(AuditMode.ASYNC_FALLBACK));
        }

        @Test
        @DisplayName("Given null extra When 호출하면 Then extra 없이 기록한다")
        void recordWithoutExtra() {
            service.auditExport("xml", 5L, null, null, null, "OK", true, AuditMode.STRICT, null);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditPort).record(captor.capture(), any());
            AuditEvent event = captor.getValue();
            // recordCount는 항상 포함됨
            assertThat(event.getExtra().get("recordCount")).isEqualTo(5L);
        }

        @Test
        @DisplayName("Given 빈 extra Map When 호출하면 Then 정상 기록된다")
        void recordWithEmptyExtra() {
            service.auditExport("xml", 5L, null, null, null, "OK", true, AuditMode.STRICT, Map.of());

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditPort).record(captor.capture(), any());
            AuditEvent event = captor.getValue();
            assertThat(event.getExtra().get("recordCount")).isEqualTo(5L);
        }

        @Test
        @DisplayName("Given AuditPort 예외 발생 When 호출하면 Then 예외 없이 완료된다")
        void exceptionIsSuppressed() {
            doThrow(new RuntimeException("Audit failed")).when(auditPort).record(any(), any());

            assertThatCode(() -> service.auditExport("excel", 1L, null, null, null, "OK", true, AuditMode.STRICT, null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Given 소문자 exportType When 호출하면 Then 대문자로 변환된 action 생성")
        void exportTypeIsUppercased() {
            service.auditExport("csv", 1L, null, null, null, "OK", true, AuditMode.STRICT, null);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditPort).record(captor.capture(), any());
            AuditEvent event = captor.getValue();
            assertThat(event.getAction()).isEqualTo("EXPORT_CSV");
        }
    }
}
