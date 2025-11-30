package com.example.admin.permission.audit;

import com.example.admin.permission.context.AuthContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Slf4jPermissionAuditLogger implements PermissionAuditLogger {

  @Override
  public void onAccessGranted(AuthContext context) {
    log.info(
        "PERMISSION_GRANTED user={} group={} feature={} action={}",
        context.username(),
        context.permissionGroupCode(),
        context.feature(),
        context.action());
  }

  @Override
  public void onAccessDenied(AuthContext context, Throwable throwable) {
    log.warn(
        "PERMISSION_DENIED user={} group={} feature={} action={}: {}",
        context != null ? context.username() : "anonymous",
        context != null ? context.permissionGroupCode() : "n/a",
        context != null ? context.feature() : "n/a",
        context != null ? context.action() : "n/a",
        throwable.getMessage());
  }
}
