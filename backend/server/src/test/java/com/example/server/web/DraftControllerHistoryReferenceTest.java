package com.example.server.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.example.admin.permission.exception.PermissionDeniedException;
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

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.admin.permission.service.PermissionEvaluator;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.draft.application.DraftApplicationService;
import com.example.draft.application.dto.DraftHistoryResponse;
import com.example.draft.application.dto.DraftReferenceResponse;
import com.example.server.config.JpaConfig;
import com.example.server.config.SecurityConfig;
import com.example.server.security.JwtAuthenticationFilter;
import com.example.server.security.RestAccessDeniedHandler;
import com.example.server.security.RestAuthenticationEntryPoint;

@WebMvcTest(controllers = DraftController.class,
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class,
                        RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class, JpaConfig.class}))
@AutoConfigureMockMvc(addFilters = false)
class DraftControllerHistoryReferenceTest {

    private static final String ORG = "ORG-001";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DraftApplicationService draftApplicationService;

    @MockBean
    private PermissionEvaluator permissionEvaluator;

    @MockBean
    private com.example.dw.application.DwOrganizationQueryService organizationQueryService;

    @BeforeEach
    void setUp() {
        AuthContextHolder.set(AuthContext.of("writer", ORG, "DEFAULT",
                FeatureCode.DRAFT, ActionCode.DRAFT_READ, com.example.common.security.RowScope.ALL));
        given(permissionEvaluator.evaluate(FeatureCode.DRAFT, ActionCode.DRAFT_AUDIT))
                .willThrow(new PermissionDeniedException("denied"));
    }

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("히스토리 조회 API는 DraftHistoryResponse 리스트를 반환한다")
    void historyList() throws Exception {
        UUID draftId = UUID.randomUUID();
        DraftHistoryResponse history = new DraftHistoryResponse(UUID.randomUUID(), "SUBMITTED", "writer", "기안 상신",
                OffsetDateTime.now(ZoneOffset.UTC));
        given(draftApplicationService.listHistory(eq(draftId), eq(ORG), eq("writer"), eq(false)))
                .willReturn(List.of(history));

        mockMvc.perform(get("/api/drafts/" + draftId + "/history").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("SUBMITTED"));
    }

    @Test
    @DisplayName("참조자 조회 API는 DraftReferenceResponse 리스트를 반환한다")
    void referencesList() throws Exception {
        UUID draftId = UUID.randomUUID();
        DraftReferenceResponse reference = new DraftReferenceResponse(UUID.randomUUID(), "user1", ORG, "writer",
                OffsetDateTime.now(ZoneOffset.UTC));
        given(draftApplicationService.listReferences(eq(draftId), eq(ORG), eq("writer"), eq(false)))
                .willReturn(List.of(reference));

        mockMvc.perform(get("/api/drafts/" + draftId + "/references").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].referencedUserId").value("user1"));
    }
}
