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
import com.example.common.security.RowScope;
import com.example.admin.approval.dto.ApprovalTemplateRootResponse;
import com.example.admin.approval.service.ApprovalTemplateRootService;
import com.example.admin.draft.repository.DraftFormTemplateRepository;
import com.example.admin.draft.repository.DraftFormTemplateRootRepository;

class TemplateAdminServiceListLineTemplateAuditNullTest {

    @Test
    @DisplayName("audit=true이고 businessType/org가 null이면 모든 라인 템플릿을 반환한다(activeOnly=false)")
    void returnsAllLinesWhenAuditAndNoFilters() {
        ApprovalTemplateRootService rootService = mock(ApprovalTemplateRootService.class);
        TemplateAdminService service = new TemplateAdminService(
                rootService,
                mock(DraftFormTemplateRepository.class),
                mock(DraftFormTemplateRootRepository.class));

        OffsetDateTime now = OffsetDateTime.now();
        List<ApprovalTemplateRootResponse> expectedList = List.of(
                new ApprovalTemplateRootResponse(UUID.randomUUID(), "TMPL-001", "a", 0, null, true, now, now, List.of()),
                new ApprovalTemplateRootResponse(UUID.randomUUID(), "TMPL-002", "b", 1, null, false, now, now, List.of())
        );
        given(rootService.list(isNull(), anyBoolean()))
                .willReturn(expectedList);

        AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, RowScope.ORG);

        List<ApprovalTemplateRootResponse> result = service.listApprovalTemplateRoots(null, null, false, ctx, true);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ApprovalTemplateRootResponse::name).containsExactlyInAnyOrder("a", "b");
    }
}
