package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.context.AuthContext;
import com.example.common.orggroup.WorkType;
import com.example.common.security.RowScope;
import com.example.draft.application.dto.DraftFormTemplateRequest;
import com.example.draft.application.dto.DraftFormTemplateResponse;
import com.example.admin.approval.service.ApprovalTemplateRootService;
import com.example.draft.domain.repository.DraftFormTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRootRepository;

class TemplateAdminServiceCreateFormTemplateInactiveTest {

    @Test
    @DisplayName("active=false로 생성하면 저장 전에 비활성화 플래그가 반영된다")
    void createFormTemplateInactive() {
        DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
        DraftFormTemplateRootRepository rootRepo = mock(DraftFormTemplateRootRepository.class);
        TemplateAdminService service = new TemplateAdminService(
                mock(ApprovalTemplateRootService.class),
                formRepo,
                rootRepo);

        given(rootRepo.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(formRepo.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        DraftFormTemplateRequest req = new DraftFormTemplateRequest("form", WorkType.GENERAL, "{}", false, null);
        AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, RowScope.ORG);

        DraftFormTemplateResponse res = service.createDraftFormTemplate(req, ctx, false);
        assertThat(res.active()).isFalse();
    }
}
