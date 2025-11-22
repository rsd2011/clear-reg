package com.example.server.audit;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.example.audit.Actor;
import com.example.audit.ActorType;
import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPort;
import com.example.audit.RiskLevel;
import com.example.auth.permission.context.AuthContextHolder;

/**
 * 감사 로그 조회 자체를 감사로 남기는 AOP.
 * AuditLogRepository 읽기 메서드 호출 시 AUDIT_ACCESS 이벤트를 적재한다.
 */
@Aspect
@Component
public class AuditLogAccessAspect {

    private final AuditPort auditPort;

    public AuditLogAccessAspect(AuditPort auditPort) {
        this.auditPort = auditPort;
    }

    @AfterReturning("execution(* com.example.audit.infra.persistence.AuditLogRepository.find*(..)) || " +
            "execution(* com.example.audit.infra.persistence.AuditLogRepository.get*(..)) || " +
            "execution(* com.example.audit.infra.persistence.AuditLogRepository.count*(..)) || " +
            "execution(* com.example.audit.infra.persistence.AuditLogRepository.exists*(..))")
    public void afterAccess(JoinPoint joinPoint) {
        var ctx = AuthContextHolder.current().orElse(null);
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
