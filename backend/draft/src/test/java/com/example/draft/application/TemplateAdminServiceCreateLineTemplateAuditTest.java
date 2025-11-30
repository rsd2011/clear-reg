package com.example.draft.application;
import com.example.admin.draft.service.TemplateAdminService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.context.AuthContext;
import com.example.common.security.RowScope;
import com.example.admin.approval.dto.ApprovalTemplateRootRequest;
import com.example.admin.approval.dto.ApprovalTemplateStepRequest;
import com.example.admin.approval.dto.ApprovalTemplateStepResponse;
import com.example.admin.approval.dto.ApprovalTemplateRootResponse;
import com.example.admin.approval.service.ApprovalTemplateRootService;
import com.example.admin.draft.repository.DraftFormTemplateRepository;
import com.example.admin.draft.repository.DraftFormTemplateRootRepository;

class TemplateAdminServiceCreateLineTemplateAuditTest {

    @Test
    @DisplayName("audit=true일 때 템플릿이 정상적으로 저장된다")
    void createLineTemplateGlobalWhenAudit() {
        ApprovalTemplateRootService rootService = mock(ApprovalTemplateRootService.class);
        TemplateAdminService service = new TemplateAdminService(
                rootService,
                mock(DraftFormTemplateRepository.class),
                mock(DraftFormTemplateRootRepository.class));

        ApprovalTemplateRootRequest req = new ApprovalTemplateRootRequest(
                "line",
                0,
                null,
                true,
                List.of(new ApprovalTemplateStepRequest(1, "GRP")));

        AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, RowScope.ORG);

        OffsetDateTime now = OffsetDateTime.now();
        ApprovalTemplateRootResponse expectedResponse = new ApprovalTemplateRootResponse(
                UUID.randomUUID(),
                "TMPL-001",
                "line",
                0,
                null,
                true,
                now,
                now,
                List.of(new ApprovalTemplateStepResponse(UUID.randomUUID(), 1, "GRP", "그룹", false))
        );
        given(rootService.create(any(ApprovalTemplateRootRequest.class), eq(ctx)))
                .willReturn(expectedResponse);

        ApprovalTemplateRootResponse res = service.createApprovalTemplateRoot(req, ctx, true);

        assertThat(res.name()).isEqualTo("line");
        assertThat(res.steps()).hasSize(1);
    }
}
