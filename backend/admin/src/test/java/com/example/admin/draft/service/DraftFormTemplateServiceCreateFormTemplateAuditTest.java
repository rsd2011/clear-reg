package com.example.admin.draft.service;
import com.example.admin.draft.service.DraftFormTemplateService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import com.example.admin.permission.context.AuthContext;
import com.example.common.orggroup.WorkType;
import com.example.admin.draft.dto.DraftFormTemplateRequest;
import com.example.admin.draft.dto.DraftFormTemplateResponse;
import com.example.admin.draft.repository.DraftFormTemplateRepository;
import com.example.admin.draft.repository.DraftFormTemplateRootRepository;

class DraftFormTemplateServiceCreateFormTemplateAuditTest {

    @Test
    @DisplayName("audit=true이면 폼 템플릿을 생성한다")
    void createFormTemplateWhenAudit() {
        DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
        DraftFormTemplateRootRepository rootRepo = mock(DraftFormTemplateRootRepository.class);
        DraftFormTemplateService service = new DraftFormTemplateService(formRepo, rootRepo);

        given(rootRepo.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(formRepo.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        DraftFormTemplateRequest req = new DraftFormTemplateRequest("form", WorkType.GENERAL, "{}", true, null);
        AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, List.of());

        DraftFormTemplateResponse res = service.createDraftFormTemplate(req, ctx, true);

        assertThat(res.name()).isEqualTo("form");
    }
}
