package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.context.AuthContext;
import com.example.common.security.RowScope;
import com.example.admin.approval.dto.ApprovalTemplateRootResponse;
import com.example.draft.application.dto.DraftFormTemplateResponse;
import com.example.draft.domain.DraftFormTemplate;
import com.example.admin.approval.repository.ApprovalTemplateRootRepository;
import com.example.admin.approval.service.ApprovalTemplateRootService;
import com.example.draft.domain.repository.DraftFormTemplateRepository;
import com.example.draft.domain.repository.DraftTemplatePresetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

class TemplateAdminServiceFilterTest {

    ApprovalTemplateRootService rootService = mock(ApprovalTemplateRootService.class);
    DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
    TemplateAdminService service = new TemplateAdminService(
            rootService,
            mock(ApprovalTemplateRootRepository.class),
            formRepo,
            mock(DraftTemplatePresetRepository.class),
            new ObjectMapper());
    AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, RowScope.ORG);

    @Test
    @DisplayName("라인 템플릿은 businessType, org, activeOnly 필터를 모두 적용한다")
    void filtersApprovalTemplateRoots() {
        OffsetDateTime now = OffsetDateTime.now();
        List<ApprovalTemplateRootResponse> expectedList = List.of(
                new ApprovalTemplateRootResponse(UUID.randomUUID(), "TMPL-001", "g", 0, null, true, now, now, List.of()),
                new ApprovalTemplateRootResponse(UUID.randomUUID(), "TMPL-002", "o1", 1, null, true, now, now, List.of())
        );
        given(rootService.list(isNull(), anyBoolean()))
                .willReturn(expectedList);

        List<ApprovalTemplateRootResponse> filtered = service.listApprovalTemplateRoots("HR", null, true, ctx, false);

        assertThat(filtered).extracting(ApprovalTemplateRootResponse::name).containsExactlyInAnyOrder("g", "o1");
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
        List<ApprovalTemplateRootResponse> expectedList = List.of(
                new ApprovalTemplateRootResponse(UUID.randomUUID(), "TMPL-001", "active", 0, null, true, now, now, List.of())
        );
        given(rootService.list(isNull(), anyBoolean()))
                .willReturn(expectedList);

        List<ApprovalTemplateRootResponse> filtered = service.listApprovalTemplateRoots(null, null, true, ctx, true);

        assertThat(filtered).extracting(ApprovalTemplateRootResponse::name).containsExactly("active");
    }
}
