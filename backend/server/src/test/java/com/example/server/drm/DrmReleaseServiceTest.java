package com.example.server.drm;

import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.example.audit.AuditMode;
import com.example.audit.drm.DrmAuditEvent;
import com.example.audit.drm.DrmAuditService;
import com.example.audit.drm.DrmEventType;

class DrmReleaseServiceTest {

    DrmAuditService auditService = Mockito.mock(DrmAuditService.class);
    DrmReleaseService service = new DrmReleaseService(auditService);

    @Test
    @DisplayName("DRM 해제 요청/승인/실행 시 감사 이벤트 3건이 기록된다")
    void requestApproveExecuteRecordAudit() {
        service.requestRelease("asset", "RC", "reason", "req", "ORG", "route", Set.of("tag"));
        service.approveRelease("asset", "appr", "RC2", "reason2", "ORG", "route", Set.of());
        service.executeRelease("asset", "actor", "ORG", "route", Set.of("t"), Instant.EPOCH);

        ArgumentCaptor<DrmAuditEvent> captor = ArgumentCaptor.forClass(DrmAuditEvent.class);
        verify(auditService, Mockito.times(3)).record(captor.capture(), Mockito.eq(AuditMode.ASYNC_FALLBACK));
        assert captor.getAllValues().stream().anyMatch(e -> e.getEventType() == DrmEventType.REQUEST);
        assert captor.getAllValues().stream().anyMatch(e -> e.getEventType() == DrmEventType.APPROVAL);
        assert captor.getAllValues().stream().anyMatch(e -> e.getEventType() == DrmEventType.EXECUTE);
    }
}
