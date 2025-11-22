package com.example.server.audit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.auth.permission.context.AuthContextHolder;
import com.example.audit.Actor;
import com.example.audit.ActorType;
import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPort;
import com.example.audit.RiskLevel;
import com.example.audit.Subject;

/**
 * 간단한 요청 단위 감사 dual-write 파일럿 인터셉터.
 * 컨트롤러 단에서 세션 사용자/엔드포인트/결과 코드를 기록한다.
 */
public class RequestAuditInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestAuditInterceptor.class);

    private final AuditPort auditPort;

    public RequestAuditInterceptor(AuditPort auditPort) {
        this.auditPort = auditPort;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("auditStart", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) {
        var ctxOpt = AuthContextHolder.getContext();
        if (ctxOpt.isEmpty()) {
            return;
        }
        var ctx = ctxOpt.get();

        boolean success = ex == null && response.getStatus() < 500;
        String resultCode = ex != null ? ex.getClass().getSimpleName() : String.valueOf(response.getStatus());

        AuditEvent event = AuditEvent.builder()
                .eventType("HTTP")
                .moduleName("server")
                .action(request.getMethod() + " " + request.getRequestURI())
                .actor(Actor.builder()
                        .id(ctx.username())
                        .type(ActorType.HUMAN)
                        .role(String.join(",", ctx.roles()))
                        .dept(ctx.organizationCode())
                        .build())
                .subject(Subject.builder()
                        .type("ORG")
                        .key(ctx.organizationCode())
                        .build())
                .channel(request.getRemoteAddr())
                .clientIp(request.getRemoteAddr())
                .userAgent(request.getHeader("User-Agent"))
                .success(success)
                .resultCode(resultCode)
                .riskLevel(RiskLevel.LOW)
                .build();

        try {
            auditPort.record(event, AuditMode.ASYNC_FALLBACK);
        } catch (RuntimeException e) {
            log.warn("Audit dual-write skipped: {}", e.getMessage());
        }
    }
}
