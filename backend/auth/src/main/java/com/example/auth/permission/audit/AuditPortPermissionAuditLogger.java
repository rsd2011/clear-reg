package com.example.auth.permission.audit;

import com.example.audit.Actor;
import com.example.audit.ActorType;
import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPort;
import com.example.audit.RiskLevel;
import com.example.auth.permission.context.AuthContext;
import org.springframework.stereotype.Component;

@Component
@org.springframework.context.annotation.Primary
public class AuditPortPermissionAuditLogger implements PermissionAuditLogger {

  private final AuditPort auditPort;

  public AuditPortPermissionAuditLogger(AuditPort auditPort) {
    this.auditPort = auditPort;
  }

  @Override
  public void onAccessGranted(AuthContext context) {
    record("PERMISSION_GRANTED", context, true, "OK");
  }

  @Override
  public void onAccessDenied(AuthContext context, Throwable throwable) {
    record(
        "PERMISSION_DENIED",
        context,
        false,
        throwable != null ? throwable.getClass().getSimpleName() : "DENIED");
  }

  private void record(String action, AuthContext ctx, boolean success, String resultCode) {
    try {
      AuditEvent event =
          AuditEvent.builder()
              .eventType("PERMISSION")
              .moduleName("auth")
              .action(action)
              .actor(
                  Actor.builder()
                      .id(ctx != null ? ctx.username() : "anonymous")
                      .type(ActorType.HUMAN)
                      .role(ctx != null ? ctx.permissionGroupCode() : null)
                      .dept(ctx != null ? ctx.organizationCode() : null)
                      .build())
              .subject(
                  com.example.audit.Subject.builder()
                      .type("FEATURE")
                      .key(ctx != null && ctx.feature() != null ? ctx.feature().name() : null)
                      .build())
              .success(success)
              .resultCode(resultCode)
              .riskLevel(success ? RiskLevel.LOW : RiskLevel.HIGH)
              .extraEntry(
                  "action", ctx != null && ctx.action() != null ? ctx.action().name() : null)
              .extraEntry(
                  "rowScope", ctx != null && ctx.rowScope() != null ? ctx.rowScope().name() : null)
              .build();
      auditPort.record(event, AuditMode.ASYNC_FALLBACK);
    } catch (Exception ignore) {
      // 감사 실패 시 접근 흐름 차단하지 않음
    }
  }
}
