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
import com.example.admin.approval.repository.ApprovalTemplateRootRepository;
import com.example.admin.approval.service.ApprovalTemplateRootService;
import com.example.draft.domain.repository.DraftFormTemplateRepository;
import com.example.draft.domain.repository.DraftTemplatePresetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

class TemplateAdminServiceListDraftFormTemplatesNoOrgTest {

    @Test
    @DisplayName("audit=false이고 org 필터가 null이면 businessType만으로 필터해 activeOnly=false로 모든 조직 결과를 반환한다")
    void filtersByBusinessTypeWhenOrgNullAndAuditFalse() {
        DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
        TemplateAdminService service = new TemplateAdminService(
                mock(ApprovalTemplateRootService.class),
                mock(ApprovalTemplateRootRepository.class),
                formRepo,
                mock(DraftTemplatePresetRepository.class),
                new ObjectMapper());

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
