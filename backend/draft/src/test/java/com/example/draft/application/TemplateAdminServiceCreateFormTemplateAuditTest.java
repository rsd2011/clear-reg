package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.auth.permission.context.AuthContext;
import com.example.common.security.RowScope;
import com.example.draft.application.request.DraftFormTemplateRequest;
import com.example.draft.application.response.DraftFormTemplateResponse;
import com.example.draft.domain.repository.ApprovalGroupRepository;
import com.example.draft.domain.repository.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

class TemplateAdminServiceCreateFormTemplateAuditTest {

    @Test
    @DisplayName("audit=true이면 organizationCode가 없을 때 글로벌 폼 템플릿을 생성한다")
    void createGlobalFormTemplateWhenAudit() {
        DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
        TemplateAdminService service = new TemplateAdminService(
                mock(ApprovalGroupRepository.class),
                mock(ApprovalLineTemplateRepository.class),
                formRepo);

        org.mockito.BDDMockito.given(formRepo.save(org.mockito.ArgumentMatchers.any())).willAnswer(invocation -> invocation.getArgument(0));

        DraftFormTemplateRequest req = new DraftFormTemplateRequest("form", "HR", null, "{}", true);
        AuthContext ctx = new AuthContext("u", "ORG1", null, null, null, RowScope.ORG, null);

        DraftFormTemplateResponse res = service.createDraftFormTemplate(req, ctx, true);

        assertThat(res.organizationCode()).isNull();
        assertThat(res.name()).isEqualTo("form");
    }
}
