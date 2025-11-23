package com.example.server.policy;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.audit.AuditMode;
import com.example.audit.AuditPort;
import com.example.auth.permission.context.AuthContextHolder;
import com.example.policy.PolicyAdminService;
import com.example.policy.dto.PolicyUpdateRequest;
import com.example.policy.dto.PolicyView;
import com.example.policy.dto.PolicyYamlRequest;

@DisplayName("PolicyAdminPortAdapter 분기 커버리지")
class PolicyAdminPortAdapterBranchTest {

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("AuthContext 없이도 정책 변경 감사가 예외 없이 기록된다")
    void recordWithAnonymousActor() {
        PolicyAdminService service = Mockito.mock(PolicyAdminService.class);
        AuditPort auditPort = Mockito.mock(AuditPort.class);
        PolicyView view = new PolicyView(false, false, false,
                java.util.List.of(), 0L, java.util.List.of(), false, 0,
                true, true, true, 30, true, "MEDIUM", true,
                java.util.List.of("/api/**"), java.util.List.of(), "before");
        PolicyView after = new PolicyView(false, false, false,
                java.util.List.of(), 0L, java.util.List.of(), false, 0,
                true, true, true, 30, true, "MEDIUM", true,
                java.util.List.of("/api/**"), java.util.List.of(), "after");
        Mockito.when(service.currentView()).thenReturn(view);
        Mockito.when(service.updateView(Mockito.any())).thenReturn(after);

        PolicyAdminPortAdapter adapter = new PolicyAdminPortAdapter(service, auditPort);
        adapter.updateToggles(new PolicyUpdateRequest(null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null));

        verify(auditPort).record(Mockito.any(), Mockito.eq(AuditMode.ASYNC_FALLBACK));
    }

    @Test
    @DisplayName("감사 기록 실패가 정책 업데이트를 막지 않는다")
    void swallowAuditException() {
        PolicyAdminService service = Mockito.mock(PolicyAdminService.class);
        AuditPort auditPort = Mockito.mock(AuditPort.class);
        PolicyView view = new PolicyView(false, false, false,
                java.util.List.of(), 0L, java.util.List.of(), false, 0,
                true, true, true, 30, true, "MEDIUM", true,
                java.util.List.of("/api/**"), java.util.List.of(), "before");
        PolicyView after = new PolicyView(false, false, false,
                java.util.List.of(), 0L, java.util.List.of(), false, 0,
                true, true, true, 30, true, "MEDIUM", true,
                java.util.List.of("/api/**"), java.util.List.of(), "after");
        Mockito.when(service.currentView()).thenReturn(view);
        Mockito.when(service.applyYamlView(Mockito.anyString())).thenReturn(after);
        doThrow(new RuntimeException("fail")).when(auditPort).record(Mockito.any(), Mockito.eq(AuditMode.ASYNC_FALLBACK));

        PolicyAdminPortAdapter adapter = new PolicyAdminPortAdapter(service, auditPort);
        adapter.updateFromYaml(new PolicyYamlRequest("after"));

        verify(auditPort, times(1)).record(Mockito.any(), Mockito.eq(AuditMode.ASYNC_FALLBACK));
    }
}
