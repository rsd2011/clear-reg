package com.example.auth.permission.audit;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.FieldMaskRule;
import com.example.auth.permission.context.AuthContext;
import com.example.common.security.RowScope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Slf4jPermissionAuditLogger 테스트")
class Slf4jPermissionAuditLoggerTest {

  private final Slf4jPermissionAuditLogger logger = new Slf4jPermissionAuditLogger();

  @Test
  @DisplayName("Given 인증 컨텍스트 When 감사 로그 호출 Then 예외 없이 처리한다")
  void givenContext_whenLogging_thenHandleGracefully() {
    AuthContext context =
        new AuthContext(
            "user",
            "ORG",
            "GROUP",
            FeatureCode.ORGANIZATION,
            ActionCode.READ,
            RowScope.ALL,
            java.util.Map.of(
                "SECRET", new FieldMaskRule("SECRET", "***", ActionCode.UNMASK, true)));
    logger.onAccessGranted(context);
    logger.onAccessDenied(context, new RuntimeException("boom"));
    logger.onAccessDenied(null, new RuntimeException("boom"));
  }
}
