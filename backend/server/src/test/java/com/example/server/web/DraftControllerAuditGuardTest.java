package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.admin.permission.exception.PermissionDeniedException;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.admin.permission.service.PermissionEvaluator;
import com.example.common.security.RowScope;
import com.example.dw.application.DwOrganizationQueryService;
import com.example.draft.application.DraftApplicationService;

class DraftControllerAuditGuardTest {

    DraftApplicationService draftApplicationService = Mockito.mock(DraftApplicationService.class);
    PermissionEvaluator permissionEvaluator = Mockito.mock(PermissionEvaluator.class);
    DwOrganizationQueryService organizationQueryService = Mockito.mock(DwOrganizationQueryService.class);

    @AfterEach
    void cleanup() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("감사 권한이 없으면 RowScope.ALL이 아닌 조직 범위를 사용한다")
    void listDrafts_usesOrgScope_whenAuditDenied() {
        when(draftApplicationService.listDrafts(any(Pageable.class), any(RowScope.class), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());
        when(organizationQueryService.getOrganizations(any(Pageable.class), any(RowScope.class), any()))
                .thenReturn(Page.empty());
        Mockito.doThrow(new PermissionDeniedException("no audit"))
                .when(permissionEvaluator).evaluate(FeatureCode.DRAFT, ActionCode.DRAFT_AUDIT);
        AuthContextHolder.set(AuthContext.of("user", "ORG1", "PG", FeatureCode.DRAFT, ActionCode.DRAFT_READ, RowScope.ORG));

        DraftController controller = new DraftController(draftApplicationService, permissionEvaluator, organizationQueryService);
        controller.listDrafts(Pageable.unpaged(), null, null, null, null);

        Mockito.verify(draftApplicationService).listDrafts(any(Pageable.class), eq(RowScope.ORG), eq("ORG1"), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("감사 권한이 있으면 RowScope.ALL로 조회한다")
    void listDrafts_usesAllScope_whenAuditEnabled() {
        when(draftApplicationService.listDrafts(any(Pageable.class), any(RowScope.class), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());
        when(organizationQueryService.getOrganizations(any(Pageable.class), any(RowScope.class), any()))
                .thenReturn(Page.empty());
        when(permissionEvaluator.evaluate(FeatureCode.DRAFT, ActionCode.DRAFT_AUDIT)).thenReturn(null);
        AuthContextHolder.set(AuthContext.of("auditor", "ORG1", "PG", FeatureCode.DRAFT, ActionCode.DRAFT_READ, RowScope.ORG));

        DraftController controller = new DraftController(draftApplicationService, permissionEvaluator, organizationQueryService);
        controller.listDrafts(Pageable.unpaged(), null, null, null, null);

        Mockito.verify(draftApplicationService).listDrafts(any(Pageable.class), eq(RowScope.ALL), eq("ORG1"), any(), any(), any(), any(), any());
    }
}
