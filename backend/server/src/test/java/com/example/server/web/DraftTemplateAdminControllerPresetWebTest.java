package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.draft.application.TemplateAdminService;
import com.example.draft.application.dto.DraftTemplatePresetRequest;
import com.example.draft.application.dto.DraftTemplatePresetResponse;
import com.example.draft.domain.TemplateScope;
import com.example.common.security.RowScope;

class DraftTemplateAdminControllerPresetWebTest {

    TemplateAdminService service = org.mockito.Mockito.mock(TemplateAdminService.class);
    DraftTemplateAdminController controller = new DraftTemplateAdminController(service);

    @Test
    @DisplayName("프리셋 생성/업데이트/목록 엔드포인트가 서비스 결과를 반환한다")
    void presetsEndpoints() {
        DraftTemplatePresetResponse response = new DraftTemplatePresetResponse(
                UUID.randomUUID(), "CODE", "name", "NOTICE", TemplateScope.ORGANIZATION,
                "ORG", "title", "content", UUID.randomUUID(), "FORM", UUID.randomUUID(),
                "APPROVAL", "{}", List.of("v"), 1, true, OffsetDateTime.now(), OffsetDateTime.now());
        given(service.createDraftTemplatePreset(any(), any(), org.mockito.ArgumentMatchers.eq(true))).willReturn(response);
        given(service.updateDraftTemplatePreset(any(), any(), any(), org.mockito.ArgumentMatchers.eq(true))).willReturn(response);
        given(service.listDraftTemplatePresets(any(), any(), any(Boolean.class), any(), org.mockito.ArgumentMatchers.eq(true))).willReturn(List.of(response));

        DraftTemplatePresetRequest req = new DraftTemplatePresetRequest("n", "NOTICE", "ORG", "t", "c",
                UUID.randomUUID(), UUID.randomUUID(), "{}", List.of("v"), true);

        com.example.admin.permission.context.AuthContextHolder.set(
                com.example.admin.permission.context.AuthContext.of("u", "ORG", "PG", com.example.admin.permission.FeatureCode.DRAFT, com.example.admin.permission.ActionCode.DRAFT_AUDIT, RowScope.ALL));

        assertThat(controller.createDraftTemplatePreset(req)).isNotNull();
        assertThat(controller.updateDraftTemplatePreset(UUID.randomUUID(), req)).isNotNull();
        assertThat(controller.listDraftTemplatePresets("NOTICE", "ORG", true)).hasSize(1);

        com.example.admin.permission.context.AuthContextHolder.clear();
    }

}
