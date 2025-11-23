package com.example.auth.permission.audit;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPort;
import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.context.AuthContext;

class AuditPortPermissionAuditLoggerTest {

    AuditPort auditPort = Mockito.mock(AuditPort.class);
    AuditPortPermissionAuditLogger logger = new AuditPortPermissionAuditLogger(auditPort);

    @Test
    @DisplayName("권한 부여 시 PERMISSION_GRANTED 감사 이벤트를 남긴다")
    void recordsAccessGranted() {
        AuthContext ctx = new AuthContext("user","ORG","PG", FeatureCode.DRAFT, ActionCode.DRAFT_READ, null,null);
        logger.onAccessGranted(ctx);
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).record(captor.capture(), Mockito.eq(AuditMode.ASYNC_FALLBACK));
    }

    @Test
    @DisplayName("권한 거부 시 PERMISSION_DENIED 감사 이벤트를 남긴다")
    void recordsAccessDenied() {
        AuthContext ctx = new AuthContext("user","ORG","PG", FeatureCode.DRAFT, ActionCode.DRAFT_READ, null,null);
        logger.onAccessDenied(ctx, new IllegalStateException("x"));
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).record(captor.capture(), Mockito.eq(AuditMode.ASYNC_FALLBACK));
    }
}
