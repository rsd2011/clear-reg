package com.example.draft.application;
import com.example.admin.draft.service.TemplateAdminService;

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
import com.example.common.orggroup.WorkType;
import com.example.common.security.RowScope;
import com.example.common.version.ChangeAction;
import com.example.admin.approval.dto.ApprovalTemplateRootResponse;
import com.example.admin.draft.dto.DraftFormTemplateResponse;
import com.example.admin.draft.domain.DraftFormTemplate;
import com.example.admin.draft.domain.DraftFormTemplateRoot;
import com.example.admin.approval.service.ApprovalTemplateRootService;
import com.example.admin.draft.repository.DraftFormTemplateRepository;
import com.example.admin.draft.repository.DraftFormTemplateRootRepository;

class TemplateAdminServiceFilterTest {

    ApprovalTemplateRootService rootService = mock(ApprovalTemplateRootService.class);
    DraftFormTemplateRepository formRepo = mock(DraftFormTemplateRepository.class);
    TemplateAdminService service = new TemplateAdminService(
            rootService,
            formRepo,
            mock(DraftFormTemplateRootRepository.class));
    AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, RowScope.ORG);

    @Test
    @DisplayName("라인 템플릿은 activeOnly 필터를 적용한다")
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
    @DisplayName("양식 템플릿은 workType, activeOnly 필터를 적용한다")
    void filtersFormTemplates() {
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);
        DraftFormTemplate active = createPublishedTemplate(root, "f1", WorkType.GENERAL, true, now);
        DraftFormTemplate inactive = createPublishedTemplate(root, "f2", WorkType.GENERAL, false, now);

        given(formRepo.findCurrentByWorkType(WorkType.GENERAL)).willReturn(List.of(active, inactive));

        List<DraftFormTemplateResponse> filtered = service.listDraftFormTemplates(WorkType.GENERAL, true, ctx, false);

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

    private DraftFormTemplate createPublishedTemplate(DraftFormTemplateRoot root, String name, WorkType workType, boolean active, OffsetDateTime now) {
        DraftFormTemplate template = DraftFormTemplate.create(
                root, 1, name, workType, "{}", active,
                ChangeAction.CREATE, null, "user", "user", now);
        // create()는 이미 PUBLISHED 상태로 생성됨
        return template;
    }
}
