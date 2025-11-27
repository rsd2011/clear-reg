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
import com.example.admin.approval.dto.ApprovalLineTemplateResponse;
import com.example.admin.approval.ApprovalLineTemplate;
import com.example.admin.approval.ApprovalGroupRepository;
import com.example.admin.approval.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

class TemplateAdminServiceListLineTemplateAuditNullTest {

    @Test
    @DisplayName("audit=true이고 businessType/org가 null이면 모든 라인 템플릿을 반환한다(activeOnly=false)")
    void returnsAllLinesWhenAuditAndNoFilters() {
        ApprovalLineTemplateRepository lineRepo = mock(ApprovalLineTemplateRepository.class);
        TemplateAdminService service = new TemplateAdminService(
                mock(ApprovalGroupRepository.class),
                lineRepo,
                mock(DraftFormTemplateRepository.class), mock(com.example.draft.domain.repository.DraftTemplatePresetRepository.class), new com.fasterxml.jackson.databind.ObjectMapper());

        OffsetDateTime now = OffsetDateTime.now();
        ApprovalLineTemplate t1 = ApprovalLineTemplate.create("a", "HR", "ORG1", now);
        ApprovalLineTemplate t2 = ApprovalLineTemplate.create("b", "FIN", null, now);
        t1.rename("a", true, now);
        t2.rename("b", false, now);
        given(lineRepo.findAll()).willReturn(List.of(t1, t2));

        AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, RowScope.ORG);

        List<ApprovalLineTemplateResponse> result = service.listApprovalLineTemplates(null, null, false, ctx, true);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ApprovalLineTemplateResponse::name).containsExactlyInAnyOrder("a", "b");
    }
}
