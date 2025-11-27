package com.example.server.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.security.RowScope;
import com.example.server.commoncode.SystemCommonCodeService;
import com.example.server.commoncode.model.CommonCodeKind;
import com.example.server.commoncode.model.SystemCommonCode;
import com.example.server.config.JpaConfig;
import com.example.server.config.SecurityConfig;
import com.example.server.security.JwtAuthenticationFilter;
import com.example.server.security.RestAccessDeniedHandler;
import com.example.server.security.RestAuthenticationEntryPoint;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = CommonCodeAdminController.class,
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class,
                        RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class, JpaConfig.class}))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CommonCodeAdminController 테스트")
class CommonCodeAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SystemCommonCodeService systemCommonCodeService;

    @BeforeEach
    void setUp() {
        AuthContextHolder.set(new AuthContext("admin", "ROOT", "ADMIN",
                FeatureCode.COMMON_CODE, ActionCode.UPDATE, RowScope.ALL, java.util.Map.of()));
    }

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("Given 공통 코드 When 조회하면 Then 전체 목록을 반환한다")
    void givenCodes_whenListing_thenReturnAll() throws Exception {
        SystemCommonCode code = SystemCommonCode.create("CATEGORY", "A", "Alpha", 1,
                CommonCodeKind.DYNAMIC, true, null, null, "tester", null);
        given(systemCommonCodeService.findAll("CATEGORY")).willReturn(List.of(code));

        mockMvc.perform(get("/api/admin/common-codes/CATEGORY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codeValue").value("A"));
    }

    @Test
    @DisplayName("Given 생성 요청 When POST 호출 Then 서비스에 위임해 신규 코드를 반환한다")
    void givenRequest_whenCreate_thenDelegateService() throws Exception {
        SystemCommonCode code = SystemCommonCode.create("CATEGORY", "B", "Bravo", 2,
                CommonCodeKind.DYNAMIC, true, null, null, "tester", null);
        given(systemCommonCodeService.create(eq("CATEGORY"), any(SystemCommonCode.class))).willReturn(code);

        mockMvc.perform(post("/api/admin/common-codes/CATEGORY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codeValue": "B",
                                  "codeName": "Bravo",
                                  "displayOrder": 2,
                                  "codeKind": "DYNAMIC",
                                  "active": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codeValue").value("B"));
    }

    @Test
    @DisplayName("Given 수정 요청 When PUT 호출 Then 서비스에 위임해 코드 정보를 갱신한다")
    void givenRequest_whenUpdate_thenDelegateService() throws Exception {
        SystemCommonCode code = SystemCommonCode.create("CATEGORY", "B", "Updated", 3,
                CommonCodeKind.DYNAMIC, false, null, null, "tester", null);
        given(systemCommonCodeService.update(eq("CATEGORY"), eq("B"), any(SystemCommonCode.class)))
                .willReturn(code);

        mockMvc.perform(put("/api/admin/common-codes/CATEGORY/B")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codeValue": "B",
                                  "codeName": "Updated",
                                  "displayOrder": 3,
                                  "codeKind": "DYNAMIC",
                                  "active": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codeName").value("Updated"));
    }
}
