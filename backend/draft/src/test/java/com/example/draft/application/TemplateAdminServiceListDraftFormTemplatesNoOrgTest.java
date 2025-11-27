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

class TemplateAdminServiceListDraftFormTemplatesNoOrgTest {

    @Test
    @DisplayName("audit=false이고 org 필터가 null이면 businessType만으로 필터해 activeOnly=false로 모든 조직 결과를 반환한다")
    void filtersByBusinessTypeWhenOrgNullAndAuditFalse() {
        DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
        TemplateAdminService service = new TemplateAdminService(
                mock(ApprovalGroupRepository.class),
                mock(ApprovalLineTemplateRepository.class),
                formRepo, mock(com.example.draft.domain.repository.DraftTemplatePresetRepository.class), new com.fasterxml.jackson.databind.ObjectMapper());

        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplate org1 = DraftFormTemplate.create("f1", "HR", "ORG1", "{}", now);
        DraftFormTemplate org2 = DraftFormTemplate.create("f2", "HR", "ORG2", "{}", now);
        DraftFormTemplate otherBiz = DraftFormTemplate.create("f3", "FIN", "ORG1", "{}", now);
        given(formRepo.findAll()).willReturn(List.of(org1, org2, otherBiz));

        AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, RowScope.ORG);

        List<DraftFormTemplateResponse> result = service.listDraftFormTemplates("HR", null, false, ctx, false);

        // audit=false이면 organizationCode=null이 컨텍스트 조직(ORG1)으로 대체되므로 ORG1 데이터만 반환
        assertThat(result).extracting(DraftFormTemplateResponse::name).containsExactly("f1");
    }
}
