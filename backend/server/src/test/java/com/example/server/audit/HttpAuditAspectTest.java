package com.example.server.audit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPort;
import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.masking.SubjectType;
import com.example.common.masking.DataKind;

@DisplayName("HttpAuditAspect")
class HttpAuditAspectTest {

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Given 정상 컨트롤러 호출 When AOP 적용 Then AuditPort에 성공 이벤트를 기록한다")
    void recordSuccess() throws Throwable {
        AuditPort auditPort = Mockito.mock(AuditPort.class);
        HttpAuditAspect aspect = new HttpAuditAspect(auditPort);

        AuthContextHolder.set(AuthContext.of("emp1", "ORG1", "ROLE_USER",
                FeatureCode.AUDIT_LOG, ActionCode.READ, null));

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/hello");
        req.setRemoteAddr("127.0.0.1");
        req.addHeader("User-Agent", "JUnit");
        MockHttpServletResponse res = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req, res));

        ProceedingJoinPoint pjp = Mockito.mock(ProceedingJoinPoint.class);
        Signature sig = Mockito.mock(Signature.class);
        when(sig.getName()).thenReturn("controllerMethod");
        when(pjp.getSignature()).thenReturn(sig);
        when(pjp.proceed()).thenReturn("ok");

        aspect.auditHttp(pjp);

        verify(auditPort, times(1))
                .record(Mockito.argThat((AuditEvent ev) ->
                                ev.isSuccess()
                                        && ev.getAction().equals("GET /api/hello")
                                        && "ROLE_USER".equals(ev.getActor().getRole())),
                        Mockito.eq(AuditMode.ASYNC_FALLBACK),
                        Mockito.argThat(mt -> mt.getSubjectType() == SubjectType.EMPLOYEE));
    }

    @Test
    @DisplayName("Given 컨트롤러 예외 발생 When AOP 적용 Then 실패 이벤트로 기록하고 예외를 전파한다")
    void recordFailure() throws Throwable {
        AuditPort auditPort = Mockito.mock(AuditPort.class);
        HttpAuditAspect aspect = new HttpAuditAspect(auditPort);

        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/fail");
        MockHttpServletResponse res = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req, res));

        ProceedingJoinPoint pjp = Mockito.mock(ProceedingJoinPoint.class);
        Signature sig = Mockito.mock(Signature.class);
        when(sig.getName()).thenReturn("controllerMethod");
        when(pjp.getSignature()).thenReturn(sig);
        when(pjp.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> aspect.auditHttp(pjp))
                .isInstanceOf(IllegalStateException.class);

        verify(auditPort, times(1))
                .record(Mockito.argThat((AuditEvent ev) ->
                                !ev.isSuccess()
                                        && "IllegalStateException".equals(ev.getResultCode())),
                        Mockito.eq(AuditMode.ASYNC_FALLBACK),
                        any());
    }

    @Test
    @DisplayName("Given RequestContext 없음 When AOP 적용 Then 기본 값으로 감사 이벤트를 기록한다")
    void recordWithoutRequestContext() throws Throwable {
        AuditPort auditPort = Mockito.mock(AuditPort.class);
        HttpAuditAspect aspect = new HttpAuditAspect(auditPort);

        ProceedingJoinPoint pjp = Mockito.mock(ProceedingJoinPoint.class);
        Signature sig = Mockito.mock(Signature.class);
        when(sig.getName()).thenReturn("controllerMethod");
        when(pjp.getSignature()).thenReturn(sig);
        when(pjp.proceed()).thenReturn("ok");

        aspect.auditHttp(pjp);

        verify(auditPort).record(Mockito.any(AuditEvent.class),
                Mockito.eq(AuditMode.ASYNC_FALLBACK),
                Mockito.argThat(mt -> mt.getSubjectType() == SubjectType.UNKNOWN));
    }

    @Test
    @DisplayName("Given MaskingTarget이 요청에 존재 When AOP 적용 Then 동일 타겟으로 기록한다")
    void reuseMaskingTargetFromRequest() throws Throwable {
        AuditPort auditPort = Mockito.mock(AuditPort.class);
        HttpAuditAspect aspect = new HttpAuditAspect(auditPort);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/customer/1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        com.example.common.masking.MaskingTarget target = com.example.common.masking.MaskingTarget.builder()
                .subjectType(com.example.common.masking.SubjectType.CUSTOMER_INDIVIDUAL)
                .dataKind(DataKind.DEFAULT)
                .defaultMask(false)
                .build();
        req.setAttribute("AUDIT_MASKING_TARGET", target);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req, res));

        ProceedingJoinPoint pjp = Mockito.mock(ProceedingJoinPoint.class);
        Signature sig = Mockito.mock(Signature.class);
        when(sig.getName()).thenReturn("controllerMethod");
        when(pjp.getSignature()).thenReturn(sig);
        when(pjp.proceed()).thenReturn("ok");

        aspect.auditHttp(pjp);

        verify(auditPort).record(Mockito.any(AuditEvent.class),
                Mockito.eq(AuditMode.ASYNC_FALLBACK),
                Mockito.eq(target));
    }
}
