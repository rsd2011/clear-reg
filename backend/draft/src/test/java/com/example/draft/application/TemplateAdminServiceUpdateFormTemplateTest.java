package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.context.AuthContext;
import com.example.common.security.RowScope;
import com.example.draft.application.dto.DraftFormTemplateRequest;
import com.example.draft.application.dto.DraftFormTemplateResponse;
import com.example.draft.domain.DraftFormTemplate;
import com.example.admin.approval.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

class TemplateAdminServiceUpdateFormTemplateTest {

    @Test
    @DisplayName("글로벌 폼 템플릿은 audit=false여도 업데이트를 허용한다")
    void updateGlobalFormTemplate() {
        DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
        TemplateAdminService service = new TemplateAdminService(
                
                mock(ApprovalLineTemplateRepository.class),
                mock(com.example.admin.approval.ApprovalGroupRepository.class),
                formRepo, mock(com.example.draft.domain.repository.DraftTemplatePresetRepository.class), new com.fasterxml.jackson.databind.ObjectMapper());

        DraftFormTemplate template = DraftFormTemplate.create("form", "HR", null, "{}", OffsetDateTime.now());
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000010");
        given(formRepo.findById(id)).willReturn(Optional.of(template));
        given(formRepo.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        DraftFormTemplateRequest req = new DraftFormTemplateRequest("form2", "HR", null, "{\"f\":1}", true);
        AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, RowScope.ORG);

        DraftFormTemplateResponse res = service.updateDraftFormTemplate(id, req, ctx, false);

        assertThat(res.name()).isEqualTo("form2");
    }
}
