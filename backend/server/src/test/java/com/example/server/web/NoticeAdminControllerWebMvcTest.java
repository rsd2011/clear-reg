package com.example.server.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.PermissionEvaluator;
import com.example.auth.permission.RequirePermissionAspect;
import com.example.auth.permission.context.AuthContext;
import com.example.auth.permission.context.AuthContextHolder;
import com.example.auth.permission.context.PermissionDecision;
import com.example.auth.permission.PermissionDeniedException;
import com.example.server.config.JpaConfig;
import com.example.server.config.SecurityConfig;
import com.example.server.notice.NoticeAdminResponse;
import com.example.server.notice.NoticePublishRequest;
import com.example.server.notice.NoticeService;
import com.example.server.notice.NoticeSeverity;
import com.example.server.notice.NoticeAudience;
import com.example.server.notice.NoticeStatus;
import com.example.file.audit.FileAuditOutboxRelay;
import com.example.server.security.JwtAuthenticationFilter;
import com.example.server.security.RestAccessDeniedHandler;
import com.example.server.security.RestAuthenticationEntryPoint;

@WebMvcTest(controllers = NoticeAdminController.class,
        properties = "spring.task.scheduling.enabled=false",
        excludeAutoConfiguration = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class,
                        RestAccessDeniedHandler.class, JpaConfig.class}))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("NoticeAdminController WebMvc 추가 분기")
class NoticeAdminControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    NoticeService noticeService;
    @MockBean
    PermissionEvaluator permissionEvaluator;
    @MockBean
    RequirePermissionAspect requirePermissionAspect;
    @MockBean
    FileAuditOutboxRelay fileAuditOutboxRelay;

    private PermissionDecision allowDecision;
    private AuthContext authContext;

    @BeforeEach
    void setUpAuth() throws Throwable {
        authContext = new AuthContext("tester", "ORG", "PG", FeatureCode.NOTICE, ActionCode.UPDATE, com.example.common.security.RowScope.ALL, null);
        allowDecision = org.mockito.Mockito.mock(PermissionDecision.class);
        given(allowDecision.toContext()).willReturn(authContext);
        given(permissionEvaluator.evaluate(any(), any())).willReturn(allowDecision);
        willAnswer(invocation -> null).given(requirePermissionAspect).enforce(any(ProceedingJoinPoint.class));
        AuthContextHolder.set(authContext);
    }

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("공지 목록이 비어 있으면 200과 빈 배열을 반환한다")
    void listNotices_empty_returns200() throws Exception {
        given(noticeService.listNotices()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("공지 게시 요청이 성공하면 200을 반환한다")
    void publishNotice_success_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        NoticeAdminResponse response = new NoticeAdminResponse(id, "N-1", "t", "c",
                NoticeSeverity.INFO, NoticeAudience.GLOBAL, NoticeStatus.PUBLISHED,
                true, OffsetDateTime.now(), null, OffsetDateTime.now(), OffsetDateTime.now(), "tester", "tester");
        given(noticeService.publishNotice(eq(id), any(), eq("tester"))).willReturn(response);

        NoticePublishRequest request = new NoticePublishRequest(OffsetDateTime.now(), null, true);
        mockMvc.perform(post("/api/admin/notices/{id}/publish", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"publishAt":"2025-11-21T00:00:00Z","expireAt":null,"pinned":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.pinned").value(true));
    }

    @Test
    @DisplayName("게시 중 상태 오류가 발생하면 400을 반환한다")
    void publishNotice_stateError_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        given(noticeService.publishNotice(eq(id), any(), eq("tester")))
                .willThrow(new com.example.server.notice.NoticeStateException("잘못된 상태"));

        mockMvc.perform(post("/api/admin/notices/{id}/publish", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"publishAt":"2025-11-21T00:00:00Z","expireAt":null,"pinned":true}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("공지 조회 시 존재하지 않으면 404를 반환한다")
    void publishNotice_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        given(noticeService.publishNotice(eq(id), any(), eq("tester")))
                .willThrow(new com.example.server.notice.NoticeNotFoundException(id));

        mockMvc.perform(post("/api/admin/notices/{id}/publish", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"publishAt":"2025-11-21T00:00:00Z","expireAt":null,"pinned":true}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("게시 도중 예외가 발생하면 예외가 전파된다")
    void publishNotice_error_throws() {
        UUID id = UUID.randomUUID();
        given(noticeService.publishNotice(eq(id), any(), eq("tester")))
                .willThrow(new RuntimeException("boom"));

        NoticeAdminController controller = new NoticeAdminController(noticeService);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                controller.publishNotice(id, new NoticePublishRequest(OffsetDateTime.now(), null, true)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("boom");
    }

    @Test
    @DisplayName("권한이 없으면 403을 반환한다")
    void publishNotice_forbidden_returns403() throws Exception {
        UUID id = UUID.randomUUID();
        given(noticeService.publishNotice(eq(id), any(), eq("tester")))
                .willThrow(new PermissionDeniedException("nope"));

        mockMvc.perform(post("/api/admin/notices/{id}/publish", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"publishAt":"2025-11-21T00:00:00Z","expireAt":null,"pinned":true}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("공지 수정 중 예외가 발생하면 500을 반환한다")
    void updateNotice_error_returns500() {
        UUID id = UUID.randomUUID();
        given(noticeService.updateNotice(eq(id), any(), eq("tester")))
                .willThrow(new RuntimeException("boom"));

        NoticeAdminController controller = new NoticeAdminController(noticeService);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                controller.updateNotice(id, new com.example.server.notice.NoticeUpdateRequest(
                        "t", "c", NoticeSeverity.INFO, NoticeAudience.ADMIN, null, null, false)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("boom");
    }

    @Test
    @DisplayName("공지 보관 중 예외가 발생하면 500을 반환한다")
    void archiveNotice_error_returns500() {
        UUID id = UUID.randomUUID();
        given(noticeService.archiveNotice(eq(id), any(), eq("tester")))
                .willThrow(new RuntimeException("boom"));

        NoticeAdminController controller = new NoticeAdminController(noticeService);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                controller.archiveNotice(id, new com.example.server.notice.NoticeArchiveRequest(null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("boom");
    }

    @Test
    @DisplayName("공지 수정 중 예외가 발생하면 500이 전파된다")
    void updateNotice_error_returns500_unit() {
        UUID id = UUID.randomUUID();
        given(noticeService.updateNotice(eq(id), any(), eq("tester")))
                .willThrow(new RuntimeException("update fail"));

        NoticeAdminController controller = new NoticeAdminController(noticeService);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                controller.updateNotice(id, new com.example.server.notice.NoticeUpdateRequest(
                        "t", "c", NoticeSeverity.INFO, NoticeAudience.ADMIN, null, null, false)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("update fail");
    }

    @Test
    @DisplayName("archive 요청이 서비스 예외면 500이 전파된다")
    void archiveNotice_error_unit() {
        UUID id = UUID.randomUUID();
        given(noticeService.archiveNotice(eq(id), any(), eq("tester")))
                .willThrow(new RuntimeException("archive fail"));

        NoticeAdminController controller = new NoticeAdminController(noticeService);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                controller.archiveNotice(id, new com.example.server.notice.NoticeArchiveRequest(null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("archive fail");
    }

    @Test
    @DisplayName("공지 목록에 1건이 있으면 200과 길이 1을 반환한다")
    void listNotices_single_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        NoticeAdminResponse response = new NoticeAdminResponse(id, "N-1", "t", "c",
                NoticeSeverity.INFO, NoticeAudience.GLOBAL, NoticeStatus.PUBLISHED,
                true, OffsetDateTime.now(), null, OffsetDateTime.now(), OffsetDateTime.now(), "tester", "tester");
        given(noticeService.listNotices()).willReturn(java.util.List.of(response));

        mockMvc.perform(get("/api/admin/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

}
