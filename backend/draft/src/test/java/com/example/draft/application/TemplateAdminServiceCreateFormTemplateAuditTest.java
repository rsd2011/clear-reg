package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.context.AuthContext;
import com.example.common.security.RowScope;
import com.example.draft.application.dto.DraftFormTemplateRequest;
import com.example.draft.application.dto.DraftFormTemplateResponse;
import com.example.admin.approval.repository.ApprovalTemplateRootRepository;
import com.example.admin.approval.service.ApprovalTemplateRootService;
import com.example.draft.domain.repository.DraftFormTemplateRepository;
import com.example.draft.domain.repository.DraftTemplatePresetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

class TemplateAdminServiceCreateFormTemplateAuditTest {

    @Test
    @DisplayName("audit=true이면 organizationCode가 없을 때 글로벌 폼 템플릿을 생성한다")
    void createGlobalFormTemplateWhenAudit() {
        DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
        TemplateAdminService service = new TemplateAdminService(
                mock(ApprovalTemplateRootService.class),
                mock(ApprovalTemplateRootRepository.class),
                formRepo,
                mock(DraftTemplatePresetRepository.class),
                new ObjectMapper());

        given(formRepo.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        DraftFormTemplateRequest req = new DraftFormTemplateRequest("form", "HR", null, "{}", true);
        AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, RowScope.ORG);

        DraftFormTemplateResponse res = service.createDraftFormTemplate(req, ctx, true);

        assertThat(res.name()).isEqualTo("form");
    }
}
