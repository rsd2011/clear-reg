package com.example.server.web;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.security.RowScope;
import com.example.admin.codemanage.dto.CodeManageItem;
import com.example.admin.codemanage.CodeManageQueryService;
import com.example.admin.codemanage.model.CodeManageKind;
import com.example.admin.codemanage.model.CodeManageSource;
import com.example.server.config.JpaConfig;
import com.example.server.config.SecurityConfig;
import com.example.server.security.JwtAuthenticationFilter;
import com.example.server.security.RestAccessDeniedHandler;
import com.example.server.security.RestAuthenticationEntryPoint;

@WebMvcTest(controllers = CommonCodeController.class,
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class,
                        RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class, JpaConfig.class}))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CommonCodeController 테스트")
class CommonCodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CodeManageQueryService codeManageQueryService;

    @BeforeEach
    void setUp() {
        AuthContextHolder.set(AuthContext.of("tester", "ORG", "DEFAULT",
                FeatureCode.COMMON_CODE, ActionCode.READ, RowScope.ALL));
    }

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("Given 공통 코드 요청 When 호출하면 Then 집계된 목록을 반환한다")
    void givenCodes_whenRequesting_thenReturnAggregatedList() throws Exception {
        CodeManageItem item = new CodeManageItem("CATEGORY", "A", "Alpha", 1, true,
                CodeManageKind.DYNAMIC, CodeManageSource.SYSTEM, "desc", null);
        given(codeManageQueryService.aggregate(eq("CATEGORY"), anyBoolean(), anyBoolean()))
                .willReturn(List.of(item));

        mockMvc.perform(get("/api/common-codes/CATEGORY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].codeValue").value("A"));
    }
}
