package com.example.server.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.example.auth.AuthService;
import com.example.auth.LoginType;
import com.example.auth.dto.AccountStatusChangeRequest;
import com.example.auth.dto.LoginResponse;
import com.example.auth.dto.PasswordChangeRequest;
import com.example.auth.dto.TokenRefreshRequest;
import com.example.auth.dto.TokenResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.server.config.SecurityConfig;
import com.example.server.config.JpaConfig;
import com.example.server.security.JwtAuthenticationFilter;
import com.example.server.security.RestAccessDeniedHandler;
import com.example.server.security.RestAuthenticationEntryPoint;

@WebMvcTest(controllers = AuthController.class,
        excludeAutoConfiguration = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class
        },
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class,
                        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
                        JpaConfig.class}))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("Given 로그인 요청 When /api/auth/login 호출 Then JWT 응답을 반환한다")
    void givenLoginRequestWhenPostThenReturnTokens() throws Exception {
        TokenResponse tokens = new TokenResponse("access", Instant.now().plusSeconds(60), "refresh", Instant.now().plusSeconds(120));
        given(authService.login(any())).willReturn(new LoginResponse("tester", LoginType.PASSWORD, tokens));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"type\":\"PASSWORD\"," +
                                "\"username\":\"tester\"," +
                                "\"password\":\"pw\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("tester"))
                .andExpect(jsonPath("$.tokens.accessToken").value("access"));
    }

    @Test
    @DisplayName("로그인 요청이 유효하지 않으면 400을 반환한다")
    void givenInvalidLoginRequestWhenPostThen400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Given 리프레시 요청 When /api/auth/refresh 호출 Then 갱신된 토큰을 반환한다")
    void givenRefreshRequestWhenPostThenReturnNewTokens() throws Exception {
        TokenResponse tokens = new TokenResponse("new-access", Instant.now().plusSeconds(60), "new-refresh", Instant.now().plusSeconds(120));
        given(authService.refreshTokens("refresh-token")).willReturn(tokens);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TokenRefreshRequest("refresh-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
    }

    @Test
    @DisplayName("Given 로그아웃 요청 When /api/auth/logout 호출 Then 204 응답을 반환한다")
    void givenLogoutRequestWhenPostThenReturnNoContent() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TokenRefreshRequest("refresh-token"))))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "tester", roles = "USER")
    @DisplayName("Given 비밀번호 변경 요청 When 인증된 사용자가 호출하면 Then 204 응답을 반환한다")
    void givenPasswordChangeWhenAuthenticatedThenReturnNoContent() throws Exception {
        mockMvc.perform(patch("/api/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordChangeRequest("oldPw1!", "NewPw2@"))))
                .andExpect(status().isNoContent());

        then(authService).should().changePassword(eq("tester"), any(PasswordChangeRequest.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("Given 계정 상태 변경 요청 When 관리자가 호출하면 Then 서비스로 위임한다")
    void givenAccountStatusPatchWhenAdminThenDelegate() throws Exception {
        AccountStatusChangeRequest request = new AccountStatusChangeRequest("user1", false);

        mockMvc.perform(patch("/api/auth/accounts/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        then(authService).should().updateAccountStatus(request);
    }

    @Test
    @DisplayName("인증되지 않은 사용자가 비밀번호 변경을 호출하면 AccessDeniedException이 발생한다")
    void changePasswordWithoutAuthThrowsAccessDenied() {
        SecurityContextHolder.clearContext();
        AuthController controller = new AuthController(authService);

        assertThatThrownBy(() -> controller.changePassword(new PasswordChangeRequest("oldPw1!", "NewPw2@")))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("Authentication required");
    }
}
