package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.util.List;

import com.example.admin.approval.repository.ApprovalGroupRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.context.AuthContext;
import com.example.common.security.RowScope;
import com.example.draft.application.dto.DraftFormTemplateResponse;
import com.example.draft.domain.DraftFormTemplate;
import com.example.admin.approval.repository.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

class TemplateAdminServiceListFormTemplateAuditNullTest {

    @Test
    @DisplayName("audit=true이고 businessType/org가 null이면 모든 폼 템플릿을 반환한다(activeOnly=false)")
    void returnsAllFormTemplatesWhenAuditAndNoFilters() {
        DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
        TemplateAdminService service = new TemplateAdminService(
                mock(ApprovalLineTemplateRepository.class),
                mock(ApprovalGroupRepository.class),
                formRepo, mock(com.example.draft.domain.repository.DraftTemplatePresetRepository.class), new com.fasterxml.jackson.databind.ObjectMapper());

        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplate f1 = DraftFormTemplate.create("f1", "HR", "ORG1", "{}", now);
        DraftFormTemplate f2 = DraftFormTemplate.create("f2", "FIN", null, "{}", now);
        f2.update("f2", "{}", false, now);
        given(formRepo.findAll()).willReturn(List.of(f1, f2));

        AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, RowScope.ORG);

        List<DraftFormTemplateResponse> result = service.listDraftFormTemplates(null, null, false, ctx, true);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(DraftFormTemplateResponse::name).containsExactlyInAnyOrder("f1", "f2");
    }
}
