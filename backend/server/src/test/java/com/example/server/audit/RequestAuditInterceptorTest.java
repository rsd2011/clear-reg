package com.example.server.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.audit.AuditPort;
import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.context.AuthContext;
import com.example.auth.permission.context.AuthContextHolder;

@DisplayName("RequestAuditInterceptor")
class RequestAuditInterceptorTest {

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("Given 인증 컨텍스트 없음 When afterCompletion Then 감사 기록을 건너뛴다")
    void skipWhenNoAuth() throws Exception {
        AuditPort auditPort = Mockito.mock(AuditPort.class);
        RequestAuditInterceptor interceptor = new RequestAuditInterceptor(auditPort);

        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse res = Mockito.mock(HttpServletResponse.class);

        interceptor.afterCompletion(req, res, new Object(), null);

        Mockito.verifyNoInteractions(auditPort);
    }

    @Test
    @DisplayName("Given 정상 요청 When afterCompletion Then ASYNC_FALLBACK로 기록한다")
    void recordSuccess() throws Exception {
        AuditPort auditPort = Mockito.mock(AuditPort.class);
        RequestAuditInterceptor interceptor = new RequestAuditInterceptor(auditPort);

        AuthContextHolder.set(new AuthContext(
                "emp1", "ORG1", "ROLE_USER",
                FeatureCode.AUDIT_LOG, ActionCode.READ, null, java.util.Map.of()
        ));

        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn("GET");
        when(req.getRequestURI()).thenReturn("/api/hello");
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");
        HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
        when(res.getStatus()).thenReturn(200);

        interceptor.afterCompletion(req, res, new Object(), null);

        verify(auditPort).record(Mockito.any(AuditEvent.class), Mockito.eq(AuditMode.ASYNC_FALLBACK), Mockito.any());
    }

    @Test
    @DisplayName("Given 예외 발생 When afterCompletion Then resultCode에 예외명을 세팅해 기록한다")
    void recordFailureWithException() throws Exception {
        AuditPort auditPort = Mockito.mock(AuditPort.class);
        RequestAuditInterceptor interceptor = new RequestAuditInterceptor(auditPort);

        AuthContextHolder.set(new AuthContext(
                "emp1", "ORG1", "ROLE_USER",
                FeatureCode.AUDIT_LOG, ActionCode.READ, null, java.util.Map.of()
        ));

        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn("POST");
        when(req.getRequestURI()).thenReturn("/api/fail");
        when(req.getRemoteAddr()).thenReturn("10.0.0.1");
        HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
        when(res.getStatus()).thenReturn(500);

        Exception ex = new IllegalStateException("boom");

        interceptor.afterCompletion(req, res, new Object(), ex);

        verify(auditPort).record(Mockito.argThat(ev -> ev.getResultCode().equals("IllegalStateException")),
                Mockito.eq(AuditMode.ASYNC_FALLBACK),
                Mockito.any());
    }

    @Test
    @DisplayName("Given 요청에 MaskingTarget 존재 When afterCompletion Then 동일 타겟으로 기록한다")
    void reuseExistingMaskingTarget() throws Exception {
        AuditPort auditPort = Mockito.mock(AuditPort.class);
        RequestAuditInterceptor interceptor = new RequestAuditInterceptor(auditPort);

        AuthContextHolder.set(new AuthContext(
                "emp2", "ORG2", "ROLE_AUDITOR",
                FeatureCode.AUDIT_LOG, ActionCode.READ, null, java.util.Map.of()
        ));

        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn("GET");
        when(req.getRequestURI()).thenReturn("/api/resource");
        when(req.getRemoteAddr()).thenReturn("127.0.0.2");
        com.example.common.masking.MaskingTarget target = com.example.common.masking.MaskingTarget.builder()
                .subjectType(com.example.common.masking.SubjectType.EMPLOYEE)
                .dataKind("HTTP")
                .defaultMask(false)
                .build();
        when(req.getAttribute("AUDIT_MASKING_TARGET")).thenReturn(target);

        HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
        when(res.getStatus()).thenReturn(503);

        interceptor.afterCompletion(req, res, new Object(), null);

        verify(auditPort).record(Mockito.any(AuditEvent.class), Mockito.eq(AuditMode.ASYNC_FALLBACK), Mockito.eq(target));
    }
}
