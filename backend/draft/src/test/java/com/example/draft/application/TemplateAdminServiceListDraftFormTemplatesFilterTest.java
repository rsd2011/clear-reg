package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.context.AuthContext;
import com.example.common.security.RowScope;
import com.example.draft.application.dto.DraftFormTemplateResponse;
import com.example.draft.domain.DraftFormTemplate;
import com.example.admin.approval.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

class TemplateAdminServiceListDraftFormTemplatesFilterTest {

    @Test
    @DisplayName("businessType 지정 + activeOnly=true일 때 해당 org의 활성 템플릿만 반환한다")
    void filtersFormTemplatesByBusinessTypeAndActive() {
        DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
        TemplateAdminService service = new TemplateAdminService(
                
                mock(ApprovalLineTemplateRepository.class),
                mock(com.example.admin.approval.ApprovalGroupRepository.class),
                formRepo, mock(com.example.draft.domain.repository.DraftTemplatePresetRepository.class), new com.fasterxml.jackson.databind.ObjectMapper());

        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplate activeOrg1 = DraftFormTemplate.create("f1", "HR", "ORG1", "{}", now);
        DraftFormTemplate inactiveOrg1 = DraftFormTemplate.create("f2", "HR", "ORG1", "{}", now);
        inactiveOrg1.update("f2", "{}", false, now);
        DraftFormTemplate otherOrg = DraftFormTemplate.create("f3", "HR", "ORG2", "{}", now);
        given(formRepo.findAll()).willReturn(List.of(activeOrg1, inactiveOrg1, otherOrg));

        AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, RowScope.ORG);

        List<DraftFormTemplateResponse> result = service.listDraftFormTemplates("HR", "ORG1", true, ctx, false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("f1");
    }
}
