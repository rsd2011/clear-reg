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
import com.example.admin.approval.ApprovalGroupRepository;
import com.example.admin.approval.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

class TemplateAdminServiceListDraftFormTemplatesActiveFalseTest {

    @Test
    @DisplayName("activeOnly=false이고 businessType+org를 지정하면 해당 조직의 모든 활성/비활성 템플릿을 반환한다")
    void returnsAllTemplatesOfOrgWhenActiveOnlyIsFalse() {
        DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
        TemplateAdminService service = new TemplateAdminService(
                mock(ApprovalGroupRepository.class),
                mock(ApprovalLineTemplateRepository.class),
                formRepo, mock(com.example.draft.domain.repository.DraftTemplatePresetRepository.class), new com.fasterxml.jackson.databind.ObjectMapper());

        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplate active = DraftFormTemplate.create("f1", "HR", "ORG1", "{}", now);
        DraftFormTemplate inactive = DraftFormTemplate.create("f2", "HR", "ORG1", "{}", now);
        inactive.update("f2", "{}", false, now);
        DraftFormTemplate otherOrg = DraftFormTemplate.create("f3", "HR", "ORG2", "{}", now);
        given(formRepo.findAll()).willReturn(List.of(active, inactive, otherOrg));

        AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, RowScope.ORG);

        List<DraftFormTemplateResponse> result = service.listDraftFormTemplates("HR", "ORG1", false, ctx, false);

        assertThat(result).extracting(DraftFormTemplateResponse::name)
                .containsExactlyInAnyOrder("f1", "f2");
    }
}
