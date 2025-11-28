package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.example.admin.approval.repository.ApprovalGroupRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.context.AuthContext;
import com.example.common.security.RowScope;
import com.example.draft.application.dto.DraftFormTemplateRequest;
import com.example.draft.application.dto.DraftFormTemplateResponse;
import com.example.admin.approval.repository.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

class TemplateAdminServiceCreateFormTemplateInactiveTest {

    @Test
    @DisplayName("active=false로 생성하면 저장 전에 비활성화 플래그가 반영된다")
    void createFormTemplateInactive() {
        DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
        TemplateAdminService service = new TemplateAdminService(
                mock(ApprovalLineTemplateRepository.class),
                mock(ApprovalGroupRepository.class),
                formRepo, mock(com.example.draft.domain.repository.DraftTemplatePresetRepository.class), new com.fasterxml.jackson.databind.ObjectMapper());

        given(formRepo.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        DraftFormTemplateRequest req = new DraftFormTemplateRequest("form", "HR", "ORG1", "{}", false);
        AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, RowScope.ORG);

        DraftFormTemplateResponse res = service.createDraftFormTemplate(req, ctx, false);
        assertThat(res.active()).isFalse();
    }
}
