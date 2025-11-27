package com.example.server.audit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPort;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.admin.permission.FeatureCode;
import com.example.admin.permission.ActionCode;

@DisplayName("AuditLogAccessAspect")
class AuditLogAccessAspectTest {

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("Given 허용된 권한 When 감사 로그 조회 Then AUDIT_ACCESS 이벤트를 적재한다")
    void allowAccessAndRecord() {
        AuditPort auditPort = Mockito.mock(AuditPort.class);
        AuditLogAccessAspect aspect = new AuditLogAccessAspect(auditPort, "AUDIT_VIEWER,COMPLIANCE_ADMIN");

        AuthContextHolder.set(AuthContext.of(
                "auditor",
                "ORG1",
                "AUDIT_VIEWER",
                FeatureCode.AUDIT_LOG,
                ActionCode.READ,
                null
        ));

        JoinPoint jp = Mockito.mock(JoinPoint.class);
        Signature sig = Mockito.mock(Signature.class);
        when(sig.getName()).thenReturn("findAll");
        when(jp.getSignature()).thenReturn(sig);

        aspect.afterAccess(jp);

        verify(auditPort, times(1)).record(any(AuditEvent.class), Mockito.eq(AuditMode.ASYNC_FALLBACK));
    }

    @Test
    @DisplayName("record 중 예외가 발생해도 조회 흐름을 막지 않는다")
    void swallowRecordingException() {
        AuditPort auditPort = Mockito.mock(AuditPort.class);
        Mockito.doThrow(new RuntimeException("kafka down"))
                .when(auditPort).record(any(AuditEvent.class), Mockito.eq(AuditMode.ASYNC_FALLBACK));
        AuditLogAccessAspect aspect = new AuditLogAccessAspect(auditPort, "AUDIT_VIEWER");

        AuthContextHolder.set(AuthContext.of(
                "auditor",
                "ORG1",
                "AUDIT_VIEWER",
                FeatureCode.AUDIT_LOG,
                ActionCode.READ,
                null
        ));

        JoinPoint jp = Mockito.mock(JoinPoint.class);
        Signature sig = Mockito.mock(Signature.class);
        when(sig.getName()).thenReturn("findById");
        when(jp.getSignature()).thenReturn(sig);

        aspect.afterAccess(jp); // 예외가 발생해도 던지지 않는다
    }

    @Test
    @DisplayName("Given 허용되지 않은 권한 When 감사 로그 조회 Then AccessDeniedException을 던진다")
    void denyWhenRoleNotAllowed() {
        AuditPort auditPort = Mockito.mock(AuditPort.class);
        AuditLogAccessAspect aspect = new AuditLogAccessAspect(auditPort, "AUDIT_VIEWER");

        AuthContextHolder.set(AuthContext.of(
                "user",
                "ORG1",
                "USER_ROLE",
                FeatureCode.AUDIT_LOG,
                ActionCode.READ,
                null
        ));

        JoinPoint jp = Mockito.mock(JoinPoint.class);
        Signature sig = Mockito.mock(Signature.class);
        when(sig.getName()).thenReturn("findById");
        when(jp.getSignature()).thenReturn(sig);

        assertThatThrownBy(() -> aspect.afterAccess(jp))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    @DisplayName("Given 허용 역할 설정이 비어 있음 When 감사 로그 조회 Then 무조건 AccessDeniedException")
    void denyWhenAllowedRolesBlank() {
        AuditPort auditPort = Mockito.mock(AuditPort.class);
        AuditLogAccessAspect aspect = new AuditLogAccessAspect(auditPort, "   ");

        AuthContextHolder.set(AuthContext.of(
                "auditor",
                "ORG1",
                "AUDIT_VIEWER",
                FeatureCode.AUDIT_LOG,
                ActionCode.READ,
                null
        ));

        JoinPoint jp = Mockito.mock(JoinPoint.class);
        Signature sig = Mockito.mock(Signature.class);
        when(sig.getName()).thenReturn("findAll");
        when(jp.getSignature()).thenReturn(sig);

        assertThatThrownBy(() -> aspect.afterAccess(jp))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
}
