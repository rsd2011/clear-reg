package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.admin.approval.TemplateScope;
import com.example.admin.approval.dto.ApprovalLineTemplateResponse;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.security.RowScope;
import com.example.draft.application.TemplateAdminService;

class DraftTemplateAdminControllerUnitCoverTest {

    TemplateAdminService service = Mockito.mock(TemplateAdminService.class);
    DraftTemplateAdminController controller = new DraftTemplateAdminController(service);

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("listApprovalLineTemplates는 현재 컨텍스트를 전달해 서비스 결과를 반환한다")
    void listApprovalLineTemplatesReturnsServiceResult() {
        AuthContextHolder.set(AuthContext.of("u", "ORG", null, null, null, RowScope.ALL));
        ApprovalLineTemplateResponse resp = new ApprovalLineTemplateResponse(
                UUID.randomUUID(),
                "CODE",
                "name",
                "BT",
                TemplateScope.ORGANIZATION,
                "ORG",
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                List.of());
        when(service.listApprovalLineTemplates(any(), any(), anyBoolean(), any(), anyBoolean())).thenReturn(List.of(resp));

        List<ApprovalLineTemplateResponse> result = controller.listApprovalLineTemplates(null, null, true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).templateCode()).isEqualTo("CODE");
    }
}
