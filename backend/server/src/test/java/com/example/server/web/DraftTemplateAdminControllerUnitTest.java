package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.draft.application.TemplateAdminService;
import com.example.draft.application.dto.DraftFormTemplateRequest;
import com.example.draft.application.dto.DraftFormTemplateResponse;
import com.example.draft.domain.TemplateScope;
import java.time.OffsetDateTime;

class DraftTemplateAdminControllerUnitTest {

    @Test
    @DisplayName("폼 템플릿 목록 조회 시 AuthContext를 전달한다")
    void listPassesAuthContext() {
        TemplateAdminService service = mock(TemplateAdminService.class);
        DraftTemplateAdminController controller = new DraftTemplateAdminController(service);
        AuthContext context = mock(AuthContext.class);
        AuthContextHolder.set(context);

        DraftFormTemplateResponse response = new DraftFormTemplateResponse(
                UUID.randomUUID(),
                "CODE",
                "이름",
                "BT",
                TemplateScope.ORGANIZATION,
                "ORG",
                "{}",
                1,
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now());
        given(service.listDraftFormTemplates(eq("BT"), eq("ORG"), eq(true), eq(context), eq(true)))
                .willReturn(List.of(response));

        List<DraftFormTemplateResponse> result = controller.listDraftFormTemplates("BT", "ORG", true);

        assertThat(result).hasSize(1);
        verify(service).listDraftFormTemplates(eq("BT"), eq("ORG"), eq(true), eq(context), eq(true));
    }
}
