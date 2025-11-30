package com.example.draft.application;

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
import com.example.draft.domain.repository.DraftFormTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRootRepository;

class TemplateAdminServiceUpdateLineTemplateTest {

    @Test
    @DisplayName("라인 템플릿 업데이트 시 이름/active/steps가 교체된다")
    void updateLineTemplateReplacesSteps() {
        ApprovalTemplateRootService rootService = mock(ApprovalTemplateRootService.class);
        TemplateAdminService service = new TemplateAdminService(
                rootService,
                mock(DraftFormTemplateRepository.class),
                mock(DraftFormTemplateRootRepository.class));

        OffsetDateTime now = OffsetDateTime.now();
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000031");

        ApprovalTemplateRootRequest req = new ApprovalTemplateRootRequest(
                "newName",
                1,
                "설명",
                true,
                List.of(
                        new ApprovalTemplateStepRequest(1, "GRP2"),
                        new ApprovalTemplateStepRequest(2, "GRP3")
                )
        );
        AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, RowScope.ORG);

        ApprovalTemplateRootResponse expectedResponse = new ApprovalTemplateRootResponse(
                id,
                "TMPL-001",
                "newName",
                1,
                "설명",
                true,
                now,
                now,
                List.of(
                        new ApprovalTemplateStepResponse(UUID.randomUUID(), 1, "GRP2", "그룹2", false),
                        new ApprovalTemplateStepResponse(UUID.randomUUID(), 2, "GRP3", "그룹3", false)
                )
        );
        given(rootService.update(eq(id), any(ApprovalTemplateRootRequest.class), eq(ctx)))
                .willReturn(expectedResponse);

        ApprovalTemplateRootResponse res = service.updateApprovalTemplateRoot(id, req, ctx, false);

        assertThat(res.name()).isEqualTo("newName");
        assertThat(res.steps()).hasSize(2);
        assertThat(res.active()).isTrue();
    }
}
