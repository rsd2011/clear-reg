package com.example.server.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.example.server.config.JpaConfig;
import com.example.server.config.SecurityConfig;
import com.example.server.security.JwtAuthenticationFilter;
import com.example.server.security.RestAccessDeniedHandler;
import com.example.server.security.RestAuthenticationEntryPoint;
import com.example.common.GreetingService;

@WebMvcTest(controllers = HelloController.class,
        excludeAutoConfiguration = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class
        },
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {JpaConfig.class, SecurityConfig.class,
                        JwtAuthenticationFilter.class,
                        RestAuthenticationEntryPoint.class,
                        RestAccessDeniedHandler.class}))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("HelloController 테스트")
class HelloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GreetingService greetingService;

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Given 인증된 사용자 When 인사 API 호출 Then 환영 메시지를 반환한다")
    void givenUserWhenGreetingThenReturnText() throws Exception {
        org.mockito.BDDMockito.given(greetingService.greet("test"))
                .willReturn("Hello test!");

        mockMvc.perform(get("/api/greeting").param("name", "test"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello test!"));
    }
}
