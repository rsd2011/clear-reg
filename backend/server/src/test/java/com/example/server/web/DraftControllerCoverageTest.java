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

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
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
}
