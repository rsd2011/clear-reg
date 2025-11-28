package com.example.server.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import com.example.admin.permission.domain.ActionCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.admin.permission.domain.FeatureCode;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.security.RowScope;
import com.example.server.config.JpaConfig;
import com.example.server.config.SecurityConfig;
import com.example.server.notification.NotificationChannel;
import com.example.server.notification.NotificationService;
import com.example.server.notification.NotificationSeverity;
import com.example.server.notification.UserNotification;
import com.example.server.security.JwtAuthenticationFilter;
import com.example.server.security.RestAccessDeniedHandler;
import com.example.server.security.RestAuthenticationEntryPoint;

@WebMvcTest(controllers = NotificationController.class,
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class,
                        RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class,
                        JpaConfig.class}))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("NotificationController 테스트")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        AuthContextHolder.set(AuthContext.of("tester", "ORG", "DEFAULT",
                FeatureCode.ALERT, ActionCode.READ,
                RowScope.ALL));
    }

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("Given 사용자 알림 When 조회 요청 Then 목록 데이터를 반환한다")
    void givenNotifications_whenListing_thenReturnData() throws Exception {
        UserNotification notification = UserNotification.create(
                "tester",
                "title",
                "message",
                NotificationSeverity.INFO,
                NotificationChannel.IN_APP,
                OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
                "system",
                null,
                null);
        given(notificationService.notificationsFor("tester"))
                .willReturn(java.util.List.of(notification));

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("title"));
    }

    @Test
    @DisplayName("Given 특정 알림 When 읽음 처리 요청 Then 서비스에서 읽음 상태로 변경한다")
    void givenNotification_whenMarkRead_thenDelegateService() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/api/notifications/" + id + "/read")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(notificationService).markAsRead(id, "tester");
    }
}
