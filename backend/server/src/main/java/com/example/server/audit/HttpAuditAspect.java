package com.example.server.audit;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.audit.Actor;
import com.example.audit.ActorType;
import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPort;
import com.example.audit.RiskLevel;
import com.example.audit.Subject;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.masking.MaskingTarget;
import com.example.common.masking.SubjectType;

/**
 * 컨트롤러 단 HTTP 감사 로깅 AOP.
 * 기존 HandlerInterceptor 기반 로깅을 대체한다.
 */
@Aspect
@Component
public class HttpAuditAspect {

    private final AuditPort auditPort;

    public HttpAuditAspect(AuditPort auditPort) {
        this.auditPort = auditPort;
    }

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object auditHttp(ProceedingJoinPoint pjp) throws Throwable {
        ServletRequestAttributes attrs = currentRequestAttributes().orElse(null);
        HttpServletRequest request = attrs != null ? attrs.getRequest() : null;
        HttpServletResponse response = attrs != null ? attrs.getResponse() : null;

        boolean success = false;
        String resultCode = "OK";
        try {
            Object ret = pjp.proceed();
            success = true;
            resultCode = response != null ? String.valueOf(response.getStatus()) : "OK";
            return ret;
        } catch (Exception ex) {
            resultCode = ex.getClass().getSimpleName();
            throw ex;
        } finally {
            buildAndRecordEvent(pjp, request, success, resultCode);
        }
    }

    private void buildAndRecordEvent(ProceedingJoinPoint pjp, HttpServletRequest request,
                                     boolean success, String resultCode) {
        var ctxOpt = AuthContextHolder.current();
        var actor = ctxOpt.map(ctx -> Actor.builder()
                        .id(ctx.username())
                        .type(ActorType.HUMAN)
                        .role(ctx.permissionGroupCode())
                        .dept(ctx.organizationCode())
                        .build())
                .orElse(Actor.builder()
                        .id("anonymous")
                        .type(ActorType.SYSTEM)
                        .build());

        String uri = request != null ? request.getRequestURI() : "unknown";
        String method = request != null ? request.getMethod() : "UNKNOWN";
        String clientIp = request != null ? request.getRemoteAddr() : null;
        String userAgent = request != null ? request.getHeader("User-Agent") : null;

        Subject subject = Subject.builder()
                .type(ctxOpt.map(c -> c.organizationCode() != null ? "ORG" : "ACCOUNT").orElse("UNKNOWN"))
                .key(ctxOpt.map(c -> c.organizationCode() != null ? c.organizationCode() : c.username()).orElse("unknown"))
                .build();

        AuditEvent event = AuditEvent.builder()
                .eventType("HTTP")
                .moduleName("server")
                .action(method + " " + uri)
                .actor(actor)
                .subject(subject)
                .channel(clientIp)
                .clientIp(clientIp)
                .userAgent(userAgent)
                .success(success)
                .resultCode(resultCode)
                .riskLevel(success ? RiskLevel.LOW : RiskLevel.MEDIUM)
                .build();

        MaskingTarget maskingTarget = request != null
                ? (MaskingTarget) request.getAttribute("AUDIT_MASKING_TARGET")
                : null;
        if (maskingTarget == null) {
            maskingTarget = MaskingTarget.builder()
                    .subjectType(ctxOpt.map(c -> c.permissionGroupCode() != null
                            ? SubjectType.EMPLOYEE : SubjectType.UNKNOWN).orElse(SubjectType.UNKNOWN))
                    .dataKind("HTTP")
                    .defaultMask(true)
                    .build();
        }
        try {
            auditPort.record(event, AuditMode.ASYNC_FALLBACK, maskingTarget);
        } catch (RuntimeException ignore) {
            // 감사 실패가 업무 트랜잭션을 막지 않도록 삼킨다.
        }
    }

    private Optional<ServletRequestAttributes> currentRequestAttributes() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servlet) {
            return Optional.of(servlet);
        }
        return Optional.empty();
    }
}
