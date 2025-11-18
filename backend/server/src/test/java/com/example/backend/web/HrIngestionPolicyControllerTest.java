package com.example.backend.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.backend.config.JpaConfig;
import com.example.backend.config.SecurityConfig;
import com.example.backend.security.JwtAuthenticationFilter;
import com.example.backend.security.RestAccessDeniedHandler;
import com.example.backend.security.RestAuthenticationEntryPoint;
import com.example.hr.application.policy.HrIngestionPolicyService;
import com.example.hr.application.policy.HrIngestionPolicyUpdateRequest;
import com.example.hr.application.policy.HrIngestionPolicyView;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = HrIngestionPolicyController.class,
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class,
                        RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class,
                        JpaConfig.class}))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("HrIngestionPolicyController")
class HrIngestionPolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HrIngestionPolicyService policyService;

    @Test
    void givenPolicy_whenGet_thenReturnView() throws Exception {
        HrIngestionPolicyView view = new HrIngestionPolicyView("0 0 1 * * *", "Asia/Seoul", Duration.ofDays(90));
        given(policyService.view()).willReturn(view);

        mockMvc.perform(get("/api/admin/hr-ingestion/policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchCron").value("0 0 1 * * *"));
    }

    @Test
    void givenRequest_whenUpdate_thenDelegate() throws Exception {
        HrIngestionPolicyUpdateRequest request = new HrIngestionPolicyUpdateRequest("0 15 2 * * *", null, Duration.ofDays(60));
        HrIngestionPolicyView view = new HrIngestionPolicyView("0 15 2 * * *", "Asia/Seoul", Duration.ofDays(60));
        given(policyService.update(request)).willReturn(view);

        mockMvc.perform(put("/api/admin/hr-ingestion/policy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retention").value("PT1440H"));

        then(policyService).should().update(request);
    }
}
