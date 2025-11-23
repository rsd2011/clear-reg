package com.example.server.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.example.policy.dto.PolicyUpdateRequest;
import com.example.policy.dto.PolicyView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.server.service.CacheMaintenanceService;
import com.example.server.policy.PolicyAdminPort;

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
@DisplayName("PolicyAdminController 테스트")
class PolicyAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PolicyAdminPort policyAdminPort;
    @MockBean
    private CacheMaintenanceService cacheMaintenanceService;

    @Test
    @DisplayName("Given 정책이 존재할 때 When 조회하면 Then 스냅샷을 반환한다")
    void givenPolicyWhenGetThenReturn() throws Exception {
        PolicyView view = new PolicyView(true, true, true, List.of("PASSWORD"),
                10_485_760L, List.of("pdf"), true, 365,
                true, true, true, 730, true, "MEDIUM", true, List.of(), List.of(),
                false, "0 0 2 1 * *", 1, "", "", 6, 60,
                true, "0 0 4 1 * *", "yaml");
        given(policyAdminPort.currentPolicy()).willReturn(view);

        mockMvc.perform(get("/api/admin/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordPolicyEnabled").value(true));
    }

    @Test
    @DisplayName("Given 업데이트 요청 When PUT 호출 Then 서비스가 정책을 갱신한다")
    void givenUpdateWhenPutThenUpdate() throws Exception {
        PolicyView view = new PolicyView(false, true, true, List.of("SSO"),
                10_485_760L, List.of("pdf"), true, 365,
                true, true, true, 730, true, "MEDIUM", true, List.of(), List.of(),
                false, "0 0 2 1 * *", 1, "", "", 6, 60,
                true, "0 0 4 1 * *", "yaml");
        PolicyUpdateRequest request = new PolicyUpdateRequest(
                false, null, null, List.of("SSO"),
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        given(policyAdminPort.updateToggles(request)).willReturn(view);

        mockMvc.perform(put("/api/admin/policies/toggles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordPolicyEnabled").value(false));

        then(policyAdminPort).should().updateToggles(request);
    }

    @Test
    @DisplayName("Given 캐시 목록 When 비우기 요청 Then 캐시 유지보수 서비스로 위임한다")
    void givenCacheListWhenClearingThenDelegate() throws Exception {
        given(cacheMaintenanceService.clearCaches(List.of("DW_EMPLOYEES"))).willReturn(List.of("DW_EMPLOYEES"));

        mockMvc.perform(post("/api/admin/policies/caches/clear")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"caches":["DW_EMPLOYEES"]}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.clearedCaches[0]").value("DW_EMPLOYEES"));

        then(cacheMaintenanceService).should().clearCaches(List.of("DW_EMPLOYEES"));
    }

    @Test
    @DisplayName("캐시 목록이 없으면 null로 호출한다")
    void givenNoBodyWhenClearingThenUsesNull() throws Exception {
        given(cacheMaintenanceService.clearCaches(null)).willReturn(List.of());

        mockMvc.perform(post("/api/admin/policies/caches/clear"))
                .andExpect(status().isAccepted());

        then(cacheMaintenanceService).should().clearCaches(null);
    }
}
