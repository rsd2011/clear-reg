package com.example.admin.permission.audit;

import static org.mockito.Mockito.verify;

import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class AuditPortPermissionAuditLoggerBranchTest {

  AuditPort port = Mockito.mock(AuditPort.class);
  AuditPortPermissionAuditLogger logger = new AuditPortPermissionAuditLogger(port);

  @Test
  @DisplayName("컨텍스트가 null이어도 예외 없이 PERMISSION_DENIED 이벤트를 기록한다")
  void recordsDeniedWhenContextNull() {
    logger.onAccessDenied(null, null);
    ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
    verify(port).record(captor.capture(), Mockito.eq(AuditMode.ASYNC_FALLBACK));
  }
}
