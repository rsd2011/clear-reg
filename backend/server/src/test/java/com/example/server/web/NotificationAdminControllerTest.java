package com.example.server.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.security.RowScope;
import com.example.server.config.JpaConfig;
import com.example.server.config.SecurityConfig;
import com.example.server.notification.NotificationService;
import com.example.server.notification.NotificationSeverity;
import com.example.server.security.JwtAuthenticationFilter;
import com.example.server.security.RestAccessDeniedHandler;
import com.example.server.security.RestAuthenticationEntryPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@WebMvcTest(controllers = NotificationAdminController.class,
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class,
                        RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class,
                        JpaConfig.class}))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("NotificationAdminController 테스트")
class NotificationAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    @BeforeEach
    void initContext() {
        AuthContextHolder.set(AuthContext.of("admin", "ORG", "ADMIN",
                FeatureCode.ALERT, ActionCode.UPDATE, RowScope.ALL));
    }

    @AfterEach
    void clearContext() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("Given 알림 발송 요청 When POST 호출 Then NotificationService로 위임한다")
    void givenSendRequest_whenPosting_thenDelegate() throws Exception {
        var request = new com.example.server.notification.dto.NotificationSendRequest(
                java.util.List.of("alice"),
                "title",
                "message",
                NotificationSeverity.INFO,
                com.example.server.notification.NotificationChannel.IN_APP,
                null,
                java.util.Map.of("key", "value"));

        mockMvc.perform(post("/api/admin/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(notificationService).send(any(), any());
    }
}
