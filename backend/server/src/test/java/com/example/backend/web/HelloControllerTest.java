package com.example.backend.web;

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

import com.example.backend.config.JpaConfig;
import com.example.backend.config.SecurityConfig;
import com.example.backend.security.JwtAuthenticationFilter;
import com.example.backend.security.RestAccessDeniedHandler;
import com.example.backend.security.RestAuthenticationEntryPoint;
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
@DisplayName("HelloController")
class HelloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GreetingService greetingService;

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Given authenticated user When greeting endpoint invoked Then return greeting text")
    void givenUserWhenGreetingThenReturnText() throws Exception {
        org.mockito.BDDMockito.given(greetingService.greet("test"))
                .willReturn("Hello test!");

        mockMvc.perform(get("/api/greeting").param("name", "test"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello test!"));
    }
}
