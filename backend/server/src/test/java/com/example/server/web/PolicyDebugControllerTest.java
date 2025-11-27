package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import com.example.common.policy.DataPolicyMatch;
import com.example.common.policy.DataPolicyProvider;
import com.example.common.policy.DataPolicyQuery;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@WebMvcTest(controllers = PolicyDebugController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
        })
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class PolicyDebugControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    PolicySettingsProvider policySettingsProvider;

    @MockBean
    DataPolicyProvider dataPolicyProvider;

    @MockBean
    com.example.admin.permission.RequirePermissionAspect aspect; // bypass security for test

    @MockBean
    com.example.auth.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @MockBean
    com.example.server.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("effective endpoint returns toggle and policy match")
    void effectiveReturnsMatch() throws Exception {
        PolicyToggleSettings toggles = new PolicyToggleSettings(false,false,false,
                java.util.List.of(),20_000_000L,java.util.List.of("pdf"),false,0,
                true,true,true,730,true,"MEDIUM",true,java.util.List.of(),java.util.List.of());
        given(policySettingsProvider.currentSettings()).willReturn(toggles);
        DataPolicyMatch match = DataPolicyMatch.builder()
                .policyId(java.util.UUID.randomUUID())
                .rowScope("ORG")
                .maskRule("PARTIAL")
                .priority(1)
                .build();
        given(dataPolicyProvider.evaluate(any(DataPolicyQuery.class))).willReturn(java.util.Optional.of(match));

        String json = mockMvc.perform(get("/api/admin/policies/effective")
                        .param("featureCode", FeatureCode.NOTICE.name())
                        .param("actionCode", ActionCode.READ.name()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(json).contains("policyToggles");
        assertThat(json).contains("maskRule");
    }
}
