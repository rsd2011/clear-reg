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
import com.example.draft.application.dto.DraftFormTemplateResponse;
import com.example.admin.approval.ApprovalLineTemplate;
import com.example.draft.domain.DraftFormTemplate;
import com.example.admin.approval.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

class TemplateAdminServiceFilterTest {

    ApprovalLineTemplateRepository lineRepo = mock(ApprovalLineTemplateRepository.class);
    DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
    TemplateAdminService service = new TemplateAdminService(lineRepo, mock(com.example.admin.approval.ApprovalGroupRepository.class), formRepo, mock(com.example.draft.domain.repository.DraftTemplatePresetRepository.class), new com.fasterxml.jackson.databind.ObjectMapper());
    AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, RowScope.ORG);

    @Test
    @DisplayName("라인 템플릿은 businessType, org, activeOnly 필터를 모두 적용한다")
    void filtersApprovalLineTemplates() {
        OffsetDateTime now = OffsetDateTime.now();
        ApprovalLineTemplate global = ApprovalLineTemplate.create("g", 0, null, now);
        ApprovalLineTemplate org1 = ApprovalLineTemplate.create("o1", 1, null, now);
        org1.rename("o1", 1, null, true, now);
        ApprovalLineTemplate org2Inactive = ApprovalLineTemplate.create("o2", 2, null, now);
        org2Inactive.rename("o2", 2, null, false, now); // inactive
        given(lineRepo.findAll()).willReturn(List.of(global, org1, org2Inactive));

        List<ApprovalLineTemplateResponse> filtered = service.listApprovalLineTemplates("HR", null, true, ctx, false);

        assertThat(filtered).extracting(ApprovalLineTemplateResponse::name).containsExactlyInAnyOrder("g", "o1");
    }

    @Test
    @DisplayName("양식 템플릿은 businessType, org, activeOnly 필터를 모두 적용한다")
    void filtersFormTemplates() {
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplate org1Active = DraftFormTemplate.create("f1", "HR", "ORG1", "{}", now);
        DraftFormTemplate org1Inactive = DraftFormTemplate.create("f2", "HR", "ORG1", "{}", now);
        org1Inactive.update("f2", "{}", false, now);
        DraftFormTemplate otherOrg = DraftFormTemplate.create("f3", "HR", "ORG2", "{}", now);
        given(formRepo.findAll()).willReturn(List.of(org1Active, org1Inactive, otherOrg));

        List<DraftFormTemplateResponse> filtered = service.listDraftFormTemplates("HR", null, true, ctx, false);

        assertThat(filtered).extracting(DraftFormTemplateResponse::name).containsExactly("f1");
    }

    @Test
    @DisplayName("라인 템플릿은 activeOnly 필터를 적용한다 (audit=true)")
    void filtersActiveOnlyWhenAuditTrue() {
        OffsetDateTime now = OffsetDateTime.now();
        ApprovalLineTemplate active = ApprovalLineTemplate.create("o1", 0, null, now);
        active.rename("active", 0, null, true, now);
        ApprovalLineTemplate inactive = ApprovalLineTemplate.create("o2", 1, null, now);
        inactive.rename("inactive", 1, null, false, now);
        given(lineRepo.findAll()).willReturn(List.of(active, inactive));

        List<ApprovalLineTemplateResponse> filtered = service.listApprovalLineTemplates(null, null, true, ctx, true);

        assertThat(filtered).extracting(ApprovalLineTemplateResponse::name).containsExactly("active");
    }
}
