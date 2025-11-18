package com.example.server.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.policy.PolicyAdminService;
import com.example.policy.dto.PolicyUpdateRequest;
import com.example.policy.dto.PolicyView;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.example.server.config.JpaConfig;
import com.example.server.config.SecurityConfig;
import com.example.server.security.JwtAuthenticationFilter;
import com.example.server.security.RestAccessDeniedHandler;
import com.example.server.security.RestAuthenticationEntryPoint;

@WebMvcTest(controllers = PolicyAdminController.class,
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class,
                        RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class,
                        JpaConfig.class}))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PolicyAdminController")
class PolicyAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PolicyAdminService policyAdminService;

    @Test
    @DisplayName("Given existing policy When get Then return snapshot")
    void givenPolicyWhenGetThenReturn() throws Exception {
        PolicyView view = new PolicyView(true, true, true, List.of("PASSWORD"), "yaml");
        given(policyAdminService.currentView()).willReturn(view);

        mockMvc.perform(get("/api/admin/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordPolicyEnabled").value(true));
    }

    @Test
    @DisplayName("Given update request When put Then update service")
    void givenUpdateWhenPutThenUpdate() throws Exception {
        PolicyView view = new PolicyView(false, true, true, List.of("SSO"), "yaml");
        PolicyUpdateRequest request = new PolicyUpdateRequest(false, null, null, List.of("SSO"));
        given(policyAdminService.updateView(request)).willReturn(view);

        mockMvc.perform(put("/api/admin/policies/toggles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordPolicyEnabled").value(false));

        then(policyAdminService).should().updateView(request);
    }
}
