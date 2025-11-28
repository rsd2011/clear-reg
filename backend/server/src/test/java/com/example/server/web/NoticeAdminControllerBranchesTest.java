package com.example.server.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.UUID;

import com.example.common.ulid.UlidUtils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.admin.permission.aop.RequirePermissionAspect;
import com.example.auth.security.JwtTokenProvider;
import com.example.server.notice.NoticeService;
import com.example.server.notice.dto.NoticeAdminResponse;

import static org.mockito.Mockito.when;

@WebMvcTest(NoticeAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(exclude = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@org.springframework.context.annotation.Import(GlobalExceptionHandler.class)
class NoticeAdminControllerBranchesTest {

    @Autowired MockMvc mockMvc;

    @MockBean NoticeService noticeService;
    @MockBean RequirePermissionAspect requirePermissionAspect;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    private NoticeAdminResponse sampleResponse(UUID id) {
        return new NoticeAdminResponse(
                id,
                "t",
                "c",
                "author",
                com.example.server.notice.NoticeSeverity.INFO,
                com.example.server.notice.NoticeAudience.GLOBAL,
                com.example.server.notice.NoticeStatus.PUBLISHED,
                false,
                java.time.OffsetDateTime.now(),
                null,
                java.time.OffsetDateTime.now(),
                java.time.OffsetDateTime.now(),
                "creator",
                "updater"
        );
    }

    @Test
    @DisplayName("공지 목록이 비어 있으면 200과 빈 배열을 반환한다")
    void listEmpty_returnsEmptyArray() throws Exception {
        when(noticeService.listNotices()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/notices"))
                .andExpect(status().isOk())
                .andExpect(content().string("[]"));
    }

    @Test
    @DisplayName("공지 발행 실패 시 500을 반환한다")
    void publishFailure_returns500() throws Exception {
        UUID id = UUID.randomUUID();
        when(noticeService.publishNotice(org.mockito.ArgumentMatchers.eq(id), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new com.example.file.FileStorageException("pub fail", null));

        mockMvc.perform(post("/api/admin/notices/" + id + "/publish")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"publishAt\":\"2025-01-01T00:00:00Z\"}"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("공지 발행 성공 시 200을 반환한다")
    void publishSuccess_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(noticeService.publishNotice(org.mockito.ArgumentMatchers.eq(id), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(sampleResponse(id));

        mockMvc.perform(post("/api/admin/notices/" + id + "/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publishAt\":\"2025-01-01T00:00:00Z\",\"pinned\":true}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("expireAt 없이 발행해도 200을 반환한다")
    void publishWithoutExpire_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(noticeService.publishNotice(org.mockito.ArgumentMatchers.eq(id), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(sampleResponse(id));

        mockMvc.perform(post("/api/admin/notices/" + id + "/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publishAt\":\"2025-01-01T00:00:00Z\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("공지 업데이트 실패 시 400을 반환한다")
    void updateFailure_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        when(noticeService.updateNotice(org.mockito.ArgumentMatchers.eq(id), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new com.example.file.FileStorageException("upd fail", null));

        mockMvc.perform(put("/api/admin/notices/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"t\",\"content\":\"c\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("공지 업데이트 성공 시 200을 반환한다")
    void updateSuccess_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(noticeService.updateNotice(org.mockito.ArgumentMatchers.eq(id), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(sampleResponse(id));

        mockMvc.perform(put("/api/admin/notices/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"content\":\"c\",\"author\":\"a\",\"audience\":\"GLOBAL\",\"pinned\":false,\"publishAt\":\"2025-01-01T00:00:00Z\",\"expireAt\":\"2025-12-31T00:00:00Z\",\"severity\":\"INFO\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("공지 아카이브 실패 시 500을 반환한다")
    void archiveFailure_returns500() throws Exception {
        UUID id = UUID.randomUUID();
        when(noticeService.archiveNotice(org.mockito.ArgumentMatchers.eq(id), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new com.example.file.FileStorageException("arch fail", null));

        mockMvc.perform(post("/api/admin/notices/" + id + "/archive")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("공지 아카이브 요청 본문이 없으면 기본 요청으로 처리한다")
    void archiveWithoutBody_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(noticeService.archiveNotice(org.mockito.ArgumentMatchers.eq(id), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(sampleResponse(id));

        mockMvc.perform(post("/api/admin/notices/" + id + "/archive"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("공지 아카이브 성공 시 200을 반환한다")
    void archiveSuccess_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(noticeService.archiveNotice(org.mockito.ArgumentMatchers.eq(id), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(sampleResponse(id));

        mockMvc.perform(post("/api/admin/notices/" + id + "/archive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("공지 목록에 항목이 있으면 200과 리스트를 반환한다")
    void listNotices_returnsItems() throws Exception {
        UUID id = UUID.randomUUID();
        when(noticeService.listNotices()).thenReturn(java.util.List.of(sampleResponse(id)));

        mockMvc.perform(get("/api/admin/notices"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(UlidUtils.toUlidString(id))));
    }
}
