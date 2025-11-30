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

class DraftFormTemplateServiceCreateFormTemplateInactiveTest {

    @Test
    @DisplayName("active=false로 생성하면 저장 전에 비활성화 플래그가 반영된다")
    void createFormTemplateInactive() {
        DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
        DraftFormTemplateRootRepository rootRepo = mock(DraftFormTemplateRootRepository.class);
        DraftFormTemplateService service = new DraftFormTemplateService(formRepo, rootRepo);

        given(rootRepo.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(formRepo.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        DraftFormTemplateRequest req = new DraftFormTemplateRequest("form", WorkType.GENERAL, "{}", false, null);
        AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, List.of());

        DraftFormTemplateResponse res = service.createDraftFormTemplate(req, ctx, false);
        assertThat(res.active()).isFalse();
    }
}
