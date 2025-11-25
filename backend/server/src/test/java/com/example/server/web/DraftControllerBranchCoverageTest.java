package com.example.server.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.PermissionDeniedException;
import com.example.auth.permission.PermissionEvaluator;
import com.example.auth.permission.context.AuthContext;
import com.example.auth.permission.context.AuthContextHolder;
import com.example.common.security.RowScope;
import com.example.draft.application.DraftApplicationService;
import com.example.draft.application.response.DraftResponse;
import com.example.draft.domain.DraftStatus;
import com.example.dw.application.DwOrganizationNode;
import com.example.dw.application.DwOrganizationQueryService;

class DraftControllerBranchCoverageTest {

    DraftApplicationService draftService = org.mockito.Mockito.mock(DraftApplicationService.class);
    PermissionEvaluator permissionEvaluator = org.mockito.Mockito.mock(PermissionEvaluator.class);
    DwOrganizationQueryService orgService = org.mockito.Mockito.mock(DwOrganizationQueryService.class);
    DraftController controller = new DraftController(draftService, permissionEvaluator, orgService);

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("rowScope null이면 기본값 ORG로 조회한다")
    void listDrafts_defaultsToOrgScope() {
        AuthContextHolder.set(new AuthContext("user", "ORG1", "PG", FeatureCode.DRAFT, ActionCode.DRAFT_READ, null, java.util.Map.of()));
        given(permissionEvaluator.evaluate(eq(FeatureCode.DRAFT), eq(ActionCode.DRAFT_AUDIT)))
                .willThrow(new PermissionDeniedException("no audit"));
        given(orgService.getOrganizations(any(Pageable.class), eq(RowScope.ORG), eq("ORG1")))
                .willReturn(new PageImpl<>(List.of(new DwOrganizationNode(UUID.randomUUID(), "ORG1", 1, "ORG1", null, "ACTIVE", java.time.LocalDate.now(), null, null, java.time.OffsetDateTime.now()))));
        given(draftService.listDrafts(any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(sampleResponse())));

        controller.listDrafts(PageRequest.of(0, 1), null, null, null, null);

        verify(draftService).listDrafts(any(), eq(RowScope.ORG), eq("ORG1"), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("감사 권한이 있으면 RowScope.ALL로 조회하며 조직 목록을 비워 전달한다")
    void listDrafts_auditUsesAllScope() {
        AuthContextHolder.set(new AuthContext("auditor", "ORG1", "PG", FeatureCode.DRAFT, ActionCode.DRAFT_READ, RowScope.ORG, java.util.Map.of()));
        given(permissionEvaluator.evaluate(eq(FeatureCode.DRAFT), eq(ActionCode.DRAFT_AUDIT))).willReturn(null);
        given(draftService.listDrafts(any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(sampleResponse())));

        controller.listDrafts(PageRequest.of(0, 1), null, null, null, null);

        verify(draftService).listDrafts(any(), eq(RowScope.ALL), eq("ORG1"), eq(List.of()), any(), any(), any(), any());
    }

    @Test
    @DisplayName("알 수 없는 FeatureCode로 템플릿 조회 시 예외를 던진다")
    void listTemplates_unknownFeatureThrows() {
        AuthContextHolder.set(new AuthContext("user", "ORG1", "PG", FeatureCode.DRAFT, ActionCode.DRAFT_CREATE, RowScope.ORG, java.util.Map.of()));
        given(permissionEvaluator.evaluate(eq(FeatureCode.DRAFT), eq(ActionCode.DRAFT_AUDIT))).willThrow(new PermissionDeniedException("no audit"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> controller.listTemplates("NOT_A_FEATURE"));

        assertEquals("알 수 없는 FeatureCode 입니다: NOT_A_FEATURE", ex.getMessage());
    }

    private DraftResponse sampleResponse() {
        return new DraftResponse(UUID.randomUUID(), "t", "c", "BF", "ORG1", "user", DraftStatus.DRAFT,
                null, "T", "F", 1, "{}", "{}", java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now(),
                null, null, null, null, List.of(), List.of(), null, null);
    }
}

