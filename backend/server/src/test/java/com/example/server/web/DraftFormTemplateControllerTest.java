package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.exception.PermissionDeniedException;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.orggroup.WorkType;
import com.example.common.security.RowScope;
import com.example.admin.draft.service.DraftFormTemplateService;
import com.example.admin.draft.dto.DraftFormTemplateResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class DraftFormTemplateControllerTest {

    DraftFormTemplateService service = mock(DraftFormTemplateService.class);
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    DraftFormTemplateController controller = new DraftFormTemplateController(service, objectMapper);

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("인증 컨텍스트가 없으면 PermissionDeniedException을 던진다")
    void throwsWhenContextMissing() {
        AuthContextHolder.clear();

        assertThatThrownBy(() -> controller.list(null, true))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    @DisplayName("서식 템플릿 목록 조회 시 workType과 activeOnly, 컨텍스트를 전달한다")
    void listDraftFormTemplatesPassesArguments() {
        AuthContext ctx = AuthContext.of("user", "ORG", "PG", null, null, RowScope.ALL);
        AuthContextHolder.set(ctx);

        DraftFormTemplateResponse resp = new DraftFormTemplateResponse(
                UUID.randomUUID(), "CODE", "이름", WorkType.HR_UPDATE, "{}", 1, true,
                null,
                com.example.common.version.VersionStatus.PUBLISHED,
                com.example.common.version.ChangeAction.CREATE, null,
                "user", "User", OffsetDateTime.now(), OffsetDateTime.now(), null,
                OffsetDateTime.now(), OffsetDateTime.now());
        when(service.listDraftFormTemplates(eq(WorkType.HR_UPDATE), eq(false), eq(ctx), eq(true))).thenReturn(List.of(resp));

        List<DraftFormTemplateResponse> result = controller.list(WorkType.HR_UPDATE, false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).templateCode()).isEqualTo("CODE");
        verify(service).listDraftFormTemplates(eq(WorkType.HR_UPDATE), eq(false), eq(ctx), eq(true));
    }

    @Test
    @DisplayName("단건 조회 시 ID로 템플릿을 조회한다")
    void getTemplateById() {
        AuthContext ctx = AuthContext.of("user", "ORG", "PG", null, null, RowScope.ALL);
        AuthContextHolder.set(ctx);

        UUID id = UUID.randomUUID();
        DraftFormTemplateResponse resp = new DraftFormTemplateResponse(
                id, "CODE", "이름", WorkType.GENERAL, "{}", 1, true,
                null,
                com.example.common.version.VersionStatus.PUBLISHED,
                com.example.common.version.ChangeAction.CREATE, null,
                "user", "User", OffsetDateTime.now(), OffsetDateTime.now(), null,
                OffsetDateTime.now(), OffsetDateTime.now());
        when(service.findById(eq(id))).thenReturn(resp);

        DraftFormTemplateResponse result = controller.get(id);

        assertThat(result.id()).isEqualTo(id);
        verify(service).findById(eq(id));
    }
}
