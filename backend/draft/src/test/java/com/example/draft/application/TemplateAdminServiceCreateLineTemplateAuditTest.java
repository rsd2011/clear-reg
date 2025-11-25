package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.auth.permission.context.AuthContext;
import com.example.common.security.RowScope;
import com.example.draft.application.request.ApprovalLineTemplateRequest;
import com.example.draft.application.request.ApprovalTemplateStepRequest;
import com.example.draft.application.response.ApprovalLineTemplateResponse;
import com.example.approval.domain.repository.ApprovalGroupRepository;
import com.example.approval.domain.repository.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

class TemplateAdminServiceCreateLineTemplateAuditTest {

    @Test
    @DisplayName("audit=true일 때 organizationCode가 null이면 글로벌 템플릿으로 저장된다")
    void createLineTemplateGlobalWhenAudit() {
        ApprovalLineTemplateRepository lineRepo = mock(ApprovalLineTemplateRepository.class);
        TemplateAdminService service = new TemplateAdminService(
                mock(ApprovalGroupRepository.class),
                lineRepo,
                mock(DraftFormTemplateRepository.class), mock(com.example.draft.domain.repository.DraftTemplatePresetRepository.class), new com.fasterxml.jackson.databind.ObjectMapper());

        ApprovalLineTemplateRequest req = new ApprovalLineTemplateRequest(
                "line",
                "HR",
                null,
                true,
                List.of(new ApprovalTemplateStepRequest(1, "GRP", "desc")));

        AuthContext ctx = new AuthContext("u", "ORG1", null, null, null, RowScope.ORG, null);

        // save가 null을 반환하지 않도록 입력 그대로 반환
        org.mockito.BDDMockito.given(lineRepo.save(org.mockito.ArgumentMatchers.any())).willAnswer(invocation -> invocation.getArgument(0));

        ApprovalLineTemplateResponse res = service.createApprovalLineTemplate(req, ctx, true);

        assertThat(res.organizationCode()).isNull();
        assertThat(res.name()).isEqualTo("line");
        assertThat(res.steps()).hasSize(1);
    }
}
