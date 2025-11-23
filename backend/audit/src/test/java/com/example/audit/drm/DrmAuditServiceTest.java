package com.example.audit.drm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPort;

class DrmAuditServiceTest {

    AuditPort auditPort = Mockito.mock(AuditPort.class);
    DrmAuditService service = new DrmAuditService(auditPort);

    @Test
    @DisplayName("DRM 이벤트를 AuditEvent로 변환해 기록한다")
    void recordDrm() {
        DrmAuditEvent drm = DrmAuditEvent.builder()
                .assetId("FILE-1")
                .eventType(DrmEventType.EXECUTE)
                .reasonCode("LEGIT")
                .reasonText("보고서 반출")
                .requestorId("alice")
                .approverId("bob")
                .expiresAt(Instant.parse("2025-12-31T00:00:00Z"))
                .route("EXPORT")
                .tags(Set.of("DRM", "EXPORT"))
                .organizationCode("ORG1")
                .build();

        service.record(drm, AuditMode.ASYNC_FALLBACK);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).record(captor.capture(), Mockito.eq(AuditMode.ASYNC_FALLBACK));

        AuditEvent event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo("DRM");
        assertThat(event.getAction()).isEqualTo("EXECUTE");
        assertThat(event.getSubject().getKey()).isEqualTo("FILE-1");
        assertThat(event.getReasonCode()).isEqualTo("LEGIT");
        assertThat(event.getExtra().get("route")).isEqualTo("EXPORT");
    }
}
