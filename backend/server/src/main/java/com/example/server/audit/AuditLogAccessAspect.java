package com.example.server.audit;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;

import com.example.audit.Actor;
import com.example.audit.ActorType;
import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPort;
import com.example.audit.RiskLevel;
import com.example.admin.permission.context.AuthContextHolder;

/**
 * 감사 로그 조회 자체를 감사로 남기는 AOP.
 * AuditLogRepository 읽기 메서드 호출 시 AUDIT_ACCESS 이벤트를 적재한다.
 */
@Aspect
@Component
public class AuditLogAccessAspect {

    private final AuditPort auditPort;
    private final java.util.Set<String> allowedRoles;

    public AuditLogAccessAspect(AuditPort auditPort,
                                @Value("${audit.access.allowed-roles:AUDIT_VIEWER}") String allowedRoles) {
        this.auditPort = auditPort;
        this.allowedRoles = java.util.Arrays.stream(allowedRoles.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toSet());
    }

    @AfterReturning("execution(* com.example.audit.infra.persistence.AuditLogRepository.find*(..)) || " +
            "execution(* com.example.audit.infra.persistence.AuditLogRepository.get*(..)) || " +
            "execution(* com.example.audit.infra.persistence.AuditLogRepository.count*(..)) || " +
            "execution(* com.example.audit.infra.persistence.AuditLogRepository.exists*(..))")
    public void afterAccess(JoinPoint joinPoint) {
        var ctx = AuthContextHolder.current().orElse(null);
        if (ctx == null || ctx.permissionGroupCode() == null || !allowedRoles.contains(ctx.permissionGroupCode())) {
            throw new AccessDeniedException("AUDIT_LOG access denied");
        }
        AuditEvent event = AuditEvent.builder()
                .eventType("AUDIT_ACCESS")
                .moduleName("audit")
                .action(joinPoint.getSignature().getName())
                .actor(Actor.builder()
                        .id(ctx != null ? ctx.username() : "unknown")
                        .type(ActorType.HUMAN)
                        .role(ctx != null ? ctx.permissionGroupCode() : null)
                        .dept(ctx != null ? ctx.organizationCode() : null)
                        .build())
                .subject(com.example.audit.Subject.builder()
                        .type("AUDIT_LOG")
                        .key("access")
                        .build())
                .success(true)
                .resultCode("OK")
                .riskLevel(RiskLevel.HIGH)
                .build();
        try {
            auditPort.record(event, AuditMode.ASYNC_FALLBACK);
        } catch (Exception ignore) {
            // 조회 실패로 업무를 막지 않는다.
        }
    }
}
