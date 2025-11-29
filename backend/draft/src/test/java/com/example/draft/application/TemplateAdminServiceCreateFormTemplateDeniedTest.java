package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.context.AuthContext;
import com.example.common.security.RowScope;
import com.example.draft.application.dto.DraftFormTemplateRequest;
import com.example.draft.domain.exception.DraftAccessDeniedException;
import com.example.admin.approval.repository.ApprovalTemplateRootRepository;
import com.example.admin.approval.service.ApprovalTemplateRootService;
import com.example.draft.domain.repository.DraftFormTemplateRepository;
import com.example.draft.domain.repository.DraftTemplatePresetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

class TemplateAdminServiceCreateFormTemplateDeniedTest {

    @Test
    @DisplayName("audit=false에서 다른 조직 코드로 폼 템플릿 생성 시 접근 거부한다")
    void createFormTemplate_deniedWhenOrgMismatch() {
        TemplateAdminService service = new TemplateAdminService(
                mock(ApprovalTemplateRootService.class),
                mock(ApprovalTemplateRootRepository.class),
                mock(DraftFormTemplateRepository.class),
                mock(DraftTemplatePresetRepository.class),
                new ObjectMapper());

        DraftFormTemplateRequest req = new DraftFormTemplateRequest("form", "HR", "ORG2", "{}", true);
        AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, RowScope.ORG);

        assertThatThrownBy(() -> service.createDraftFormTemplate(req, ctx, false))
                .isInstanceOf(DraftAccessDeniedException.class);
    }
}
