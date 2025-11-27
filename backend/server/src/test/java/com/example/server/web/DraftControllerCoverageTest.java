package com.example.server.web;

import static org.mockito.ArgumentMatchers.*;
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

import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import com.example.admin.permission.PermissionEvaluator;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.security.RowScope;
import com.example.draft.application.DraftApplicationService;
import com.example.draft.application.dto.DraftResponse;
import com.example.draft.domain.DraftStatus;
import com.example.dw.application.DwOrganizationNode;
import com.example.dw.application.DwOrganizationQueryService;

class DraftControllerCoverageTest {

    DraftApplicationService draftService = org.mockito.Mockito.mock(DraftApplicationService.class);
    PermissionEvaluator permissionEvaluator = org.mockito.Mockito.mock(PermissionEvaluator.class);
    DwOrganizationQueryService orgService = org.mockito.Mockito.mock(DwOrganizationQueryService.class);
    DraftController controller = new DraftController(draftService, permissionEvaluator, orgService);

    private DraftResponse sampleResponse() {
        return new DraftResponse(UUID.randomUUID(), "t", "c", "DRAFT", "ORG1", "user", DraftStatus.DRAFT,
                null, "T", "F", 1, "{}", "{}", java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now(),
                null, null, null, null, List.of(), List.of(), null, null);
    }

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("인증 컨텍스트가 없으면 PermissionDeniedException을 던진다")
    void listDrafts_noAuthContext_throws() {
        // given - AuthContextHolder에 아무것도 설정하지 않음
        AuthContextHolder.clear();

        // when & then
        org.junit.jupiter.api.Assertions.assertThrows(
                com.example.admin.permission.PermissionDeniedException.class,
                () -> controller.listDrafts(PageRequest.of(0, 1), null, null, null, null)
        );
    }

    @Test
    @DisplayName("RowScope.ORG는 그대로 사용된다 (OWN이 아닌 경우)")
    void listDrafts_orgScopePassesThrough() {
        AuthContextHolder.set(AuthContext.of("user", "ORG1", "PG", FeatureCode.DRAFT, ActionCode.DRAFT_READ, RowScope.ORG));
        given(permissionEvaluator.evaluate(eq(FeatureCode.DRAFT), eq(ActionCode.DRAFT_AUDIT)))
                .willThrow(new com.example.admin.permission.PermissionDeniedException("no audit"));
        given(orgService.getOrganizations(any(Pageable.class), eq(RowScope.ORG), eq("ORG1")))
                .willReturn(new PageImpl<>(List.of(new DwOrganizationNode(UUID.randomUUID(), "ORG1", 1, "ORG1", null, "ACTIVE", java.time.LocalDate.now(), null, null, java.time.OffsetDateTime.now()))));
        given(draftService.listDrafts(any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(new DraftResponse(UUID.randomUUID(), "t", "c", "BF", "ORG1", "user", DraftStatus.DRAFT, null, "T", "F", 1, "{}", "{}", java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now(), null, null, null, null, List.of(), List.of(), null, null))));

        controller.listDrafts(PageRequest.of(0, 1), null, null, null, null);

        verify(draftService).listDrafts(any(), eq(RowScope.ORG), eq("ORG1"), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("RowScope OWN는 ORG로 정규화되어 listDrafts에 전달된다")
    void listDrafts_normalizesOwnScope() {
        AuthContextHolder.set(AuthContext.of("user", "ORG1", "PG", FeatureCode.DRAFT, ActionCode.DRAFT_READ, RowScope.OWN));
        given(permissionEvaluator.evaluate(eq(FeatureCode.DRAFT), eq(ActionCode.DRAFT_AUDIT)))
                .willThrow(new com.example.admin.permission.PermissionDeniedException("no audit"));
        given(orgService.getOrganizations(any(Pageable.class), eq(RowScope.ORG), eq("ORG1")))
                .willReturn(new PageImpl<>(List.of(new DwOrganizationNode(UUID.randomUUID(), "ORG1", 1, "ORG1", null, "ACTIVE", java.time.LocalDate.now(), null, null, java.time.OffsetDateTime.now()))));
        given(draftService.listDrafts(any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(new DraftResponse(UUID.randomUUID(), "t", "c", "BF", "ORG1", "user", DraftStatus.DRAFT, null, "T", "F", 1, "{}", "{}", java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now(), null, null, null, null, List.of(), List.of(), null, null))));

        controller.listDrafts(PageRequest.of(0,1), null, null, null, null);

        verify(draftService).listDrafts(any(), eq(RowScope.ORG), eq("ORG1"), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("delegateDraft 호출 시 서비스로 위임한다")
    void delegateDraft_delegatesToService() {
        UUID id = UUID.randomUUID();
        AuthContextHolder.set(AuthContext.of("user", "ORG1", "PG", FeatureCode.DRAFT, ActionCode.DRAFT_DELEGATE, RowScope.ORG));
        given(permissionEvaluator.evaluate(eq(FeatureCode.DRAFT), eq(ActionCode.DRAFT_AUDIT)))
                .willThrow(new com.example.admin.permission.PermissionDeniedException("no"));
        given(draftService.getDraft(eq(id), eq("ORG1"), eq("user"), eq(false))).willReturn(sampleResponse());
        given(draftService.delegate(any(), any(), any(), any(), any(), eq(false))).willReturn(sampleResponse());

        controller.delegateDraft(id, new com.example.draft.application.dto.DraftDecisionRequest(UUID.randomUUID(), "comment"), "delegatee");

        verify(draftService).delegate(eq(id), any(), eq("delegatee"), eq("user"), eq("ORG1"), eq(false));
    }

    @Test
    @DisplayName("approveDeferredDraft 호출 시 서비스로 위임한다")
    void approveDeferredDraft_delegatesToService() {
        UUID id = UUID.randomUUID();
        AuthContextHolder.set(AuthContext.of("user", "ORG1", "PG", FeatureCode.DRAFT, ActionCode.DRAFT_APPROVE, RowScope.ORG));
        given(permissionEvaluator.evaluate(eq(FeatureCode.DRAFT), eq(ActionCode.DRAFT_AUDIT)))
                .willThrow(new com.example.admin.permission.PermissionDeniedException("no"));
        given(draftService.getDraft(eq(id), eq("ORG1"), eq("user"), eq(false))).willReturn(sampleResponse());
        given(draftService.approveDeferred(any(), any(), any(), any(), eq(false))).willReturn(sampleResponse());

        controller.approveDeferredDraft(id, new com.example.draft.application.dto.DraftDecisionRequest(UUID.randomUUID(), "approved"));

        verify(draftService).approveDeferred(eq(id), any(), eq("user"), eq("ORG1"), eq(false));
    }

    @Test
    @DisplayName("defaultTemplates 호출 시 서비스로 위임한다")
    void defaultTemplates_delegatesToService() {
        AuthContextHolder.set(AuthContext.of("user", "ORG1", "PG", FeatureCode.DRAFT, ActionCode.DRAFT_CREATE, RowScope.ORG));
        given(permissionEvaluator.evaluate(eq(FeatureCode.DRAFT), eq(ActionCode.DRAFT_AUDIT)))
                .willThrow(new com.example.admin.permission.PermissionDeniedException("no"));
        given(draftService.suggestTemplate(eq("DRAFT"), eq("ORG1")))
                .willReturn(new com.example.draft.application.dto.DraftTemplateSuggestionResponse(null, null, null, null, false));

        controller.defaultTemplates("DRAFT");

        verify(draftService).suggestTemplate(eq("DRAFT"), eq("ORG1"));
    }

    @Test
    @DisplayName("recommendTemplates 호출 시 서비스로 위임한다")
    void recommendTemplates_delegatesToService() {
        AuthContextHolder.set(AuthContext.of("user", "ORG1", "PG", FeatureCode.DRAFT, ActionCode.DRAFT_CREATE, RowScope.ORG));
        given(permissionEvaluator.evaluate(eq(FeatureCode.DRAFT), eq(ActionCode.DRAFT_AUDIT)))
                .willThrow(new com.example.admin.permission.PermissionDeniedException("no"));
        given(draftService.recommendTemplatePresets(eq("DRAFT"), eq("ORG1"), eq("user")))
                .willReturn(List.of());

        controller.recommendTemplates("DRAFT");

        verify(draftService).recommendTemplatePresets(eq("DRAFT"), eq("ORG1"), eq("user"));
    }

    @Test
    @DisplayName("audit 호출 시 서비스로 위임한다")
    void audit_delegatesToService() {
        UUID id = UUID.randomUUID();
        AuthContextHolder.set(AuthContext.of("user", "ORG1", "PG", FeatureCode.DRAFT, ActionCode.DRAFT_AUDIT, RowScope.ALL));
        given(draftService.listAudit(eq(id), eq("ORG1"), eq("user"), eq(true), any(), any(), any(), any()))
                .willReturn(List.of());

        controller.audit(id, null, null, null, null);

        verify(draftService).listAudit(eq(id), eq("ORG1"), eq("user"), eq(true), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    @DisplayName("getDraft 호출 시 서비스로 위임한다")
    void getDraft_delegatesToService() {
        UUID id = UUID.randomUUID();
        AuthContextHolder.set(AuthContext.of("user", "ORG1", "PG", FeatureCode.DRAFT, ActionCode.DRAFT_READ, RowScope.ORG));
        given(permissionEvaluator.evaluate(eq(FeatureCode.DRAFT), eq(ActionCode.DRAFT_AUDIT)))
                .willThrow(new com.example.admin.permission.PermissionDeniedException("no"));
        given(draftService.getDraft(eq(id), eq("ORG1"), eq("user"), eq(false))).willReturn(sampleResponse());

        controller.getDraft(id);

        verify(draftService).getDraft(eq(id), eq("ORG1"), eq("user"), eq(false));
    }
}
