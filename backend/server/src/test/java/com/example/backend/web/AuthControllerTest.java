package com.example.backend.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.MockMvc;

import com.example.auth.AuthService;
import com.example.auth.LoginType;
import com.example.auth.dto.LoginResponse;
import com.example.auth.dto.TokenRefreshRequest;
import com.example.auth.dto.TokenResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.backend.config.SecurityConfig;
import com.example.backend.config.JpaConfig;
import com.example.backend.security.JwtAuthenticationFilter;
import com.example.backend.security.RestAccessDeniedHandler;
import com.example.backend.security.RestAuthenticationEntryPoint;

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
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("Given login request When posting to /api/auth/login Then return JWT payload")
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
    @DisplayName("Given refresh request When posting to /api/auth/refresh Then return refreshed tokens")
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
    @DisplayName("Given logout request When posting to /api/auth/logout Then return 204")
    void givenLogoutRequestWhenPostThenReturnNoContent() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TokenRefreshRequest("refresh-token"))))
                .andExpect(status().isNoContent());
    }
}
