package com.example.auth.permission.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.auth.permission.context.AuthContext;

@Component
public class Slf4jPermissionAuditLogger implements PermissionAuditLogger {

    private static final Logger log = LoggerFactory.getLogger(Slf4jPermissionAuditLogger.class);

    @Override
    public void onAccessGranted(AuthContext context) {
        log.info("PERMISSION_GRANTED user={} group={} feature={} action={} scope={}",
                context.username(), context.permissionGroupCode(), context.feature(), context.action(), context.rowScope());
    }

    @Override
    public void onAccessDenied(AuthContext context, Throwable throwable) {
        log.warn("PERMISSION_DENIED user={} group={} feature={} action={}: {}",
                context != null ? context.username() : "anonymous",
                context != null ? context.permissionGroupCode() : "n/a",
                context != null ? context.feature() : "n/a",
                context != null ? context.action() : "n/a",
                throwable.getMessage());
    }
}
