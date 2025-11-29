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

class TemplateAdminServiceCreateFormTemplateInactiveTest {

    @Test
    @DisplayName("active=false로 생성하면 저장 전에 비활성화 플래그가 반영된다")
    void createFormTemplateInactive() {
        DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
        TemplateAdminService service = new TemplateAdminService(
                mock(ApprovalTemplateRootService.class),
                mock(ApprovalTemplateRootRepository.class),
                formRepo,
                mock(DraftTemplatePresetRepository.class),
                new ObjectMapper());

        given(formRepo.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        DraftFormTemplateRequest req = new DraftFormTemplateRequest("form", "HR", "ORG1", "{}", false);
        AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, RowScope.ORG);

        DraftFormTemplateResponse res = service.createDraftFormTemplate(req, ctx, false);
        assertThat(res.active()).isFalse();
    }
}
