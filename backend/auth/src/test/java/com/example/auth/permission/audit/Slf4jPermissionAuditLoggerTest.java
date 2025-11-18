package com.example.auth.permission.audit;

import org.junit.jupiter.api.Test;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.FieldMaskRule;
import com.example.auth.permission.context.AuthContext;
import com.example.common.security.RowScope;

class Slf4jPermissionAuditLoggerTest {

    private final Slf4jPermissionAuditLogger logger = new Slf4jPermissionAuditLogger();

    @Test
    void givenContext_whenLogging_thenHandleGracefully() {
        AuthContext context = new AuthContext("user", "ORG", "GROUP", FeatureCode.ORGANIZATION,
                ActionCode.READ, RowScope.ALL, java.util.Map.of("SECRET", new FieldMaskRule("SECRET", "***", ActionCode.UNMASK, true)));
        logger.onAccessGranted(context);
        logger.onAccessDenied(context, new RuntimeException("boom"));
        logger.onAccessDenied(null, new RuntimeException("boom"));
    }
}
