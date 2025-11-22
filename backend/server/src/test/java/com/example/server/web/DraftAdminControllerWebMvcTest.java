package com.example.server.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
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
import org.springframework.test.web.servlet.MockMvc;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.PermissionEvaluator;
import com.example.auth.permission.PermissionDeniedException;
import com.example.auth.permission.RequirePermissionAspect;
import com.example.auth.permission.context.AuthContext;
import com.example.auth.permission.context.AuthContextHolder;
import com.example.auth.permission.context.PermissionDecision;
import com.example.draft.domain.ApprovalGroup;
import com.example.draft.domain.ApprovalLineTemplate;
import com.example.draft.domain.DraftFormTemplate;
import com.example.draft.domain.TemplateScope;
import com.example.draft.domain.repository.ApprovalGroupRepository;
import com.example.draft.domain.repository.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;
import com.example.file.audit.FileAuditOutboxRelay;
import com.example.server.config.JpaConfig;
import com.example.server.config.SecurityConfig;
import com.example.server.security.JwtAuthenticationFilter;
import com.example.server.security.RestAccessDeniedHandler;
import com.example.server.security.RestAuthenticationEntryPoint;

@WebMvcTest(controllers = DraftAdminController.class,
        properties = "spring.task.scheduling.enabled=false",
        excludeAutoConfiguration = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class,
                        RestAccessDeniedHandler.class, JpaConfig.class}))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("DraftAdminController WebMvc 분기")
class DraftAdminControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ApprovalLineTemplateRepository approvalRepo;
    @MockBean
    DraftFormTemplateRepository formRepo;
    @MockBean
    ApprovalGroupRepository groupRepo;
    @MockBean
    PermissionEvaluator permissionEvaluator;
    @MockBean
    RequirePermissionAspect requirePermissionAspect;
    @MockBean
    FileAuditOutboxRelay fileAuditOutboxRelay;

    @Test
    @DisplayName("approval 템플릿 businessType 필터가 적용된다")
    void approvalTemplates_filtersBusinessType() throws Exception, Throwable {
        seedAuth();
        ApprovalLineTemplate t1 = ApprovalLineTemplate.create("name1", "HR", "ORG", OffsetDateTime.now());
        ApprovalLineTemplate t2 = ApprovalLineTemplate.create("name2", "IT", "ORG", OffsetDateTime.now());
        given(approvalRepo.findAll()).willReturn(List.of(t1, t2));
        PermissionDecision decision = org.mockito.Mockito.mock(PermissionDecision.class);
        given(decision.toContext()).willReturn(AuthContextHolder.current().orElse(null));
        given(permissionEvaluator.evaluate(any(), any())).willReturn(decision);
        willAnswer(invocation -> null).given(requirePermissionAspect).enforce(any(ProceedingJoinPoint.class));

        mockMvc.perform(get("/api/admin/draft/templates/approval").param("businessType", "HR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("그룹 조회 시 조직 코드 필터가 적용된다")
    void groups_filtersOrganization() throws Exception, Throwable {
        seedAuth();
        ApprovalGroup g1 = ApprovalGroup.create("G1", "n1", "d1", "ORG1", null, OffsetDateTime.now());
        ApprovalGroup g2 = ApprovalGroup.create("G2", "n2", "d2", "ORG2", null, OffsetDateTime.now());
        given(groupRepo.findAll()).willReturn(List.of(g1, g2));
        PermissionDecision decision = org.mockito.Mockito.mock(PermissionDecision.class);
        given(decision.toContext()).willReturn(AuthContextHolder.current().orElse(null));
        given(permissionEvaluator.evaluate(any(), any())).willReturn(decision);
        willAnswer(invocation -> null).given(requirePermissionAspect).enforce(any(ProceedingJoinPoint.class));

        mockMvc.perform(get("/api/admin/draft/groups").param("organizationCode", "ORG1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].organizationCode").value("ORG1"));
    }

    @Test
    @DisplayName("businessType 필터가 적용되어 1건만 반환된다")
    void formTemplates_filter_businessType() throws Exception, Throwable {
        seedAuth();
        DraftFormTemplate f1 = DraftFormTemplate.create("hr", "HR", "ORG", "{}", OffsetDateTime.now());
        DraftFormTemplate f2 = DraftFormTemplate.create("it", "IT", "ORG", "{}", OffsetDateTime.now());
        given(formRepo.findAll()).willReturn(List.of(f1, f2));
        PermissionDecision decision = org.mockito.Mockito.mock(PermissionDecision.class);
        given(decision.toContext()).willReturn(AuthContextHolder.current().orElse(null));
        given(permissionEvaluator.evaluate(any(), any())).willReturn(decision);
        willAnswer(invocation -> null).given(requirePermissionAspect).enforce(any(ProceedingJoinPoint.class));

        mockMvc.perform(get("/api/admin/draft/templates/form").param("businessType", "HR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].businessType").value("HR"));
    }

    private void seedAuth() {
        AuthContext context = new AuthContext("tester", "ORG", "PG", FeatureCode.DRAFT, ActionCode.DRAFT_AUDIT, com.example.common.security.RowScope.ALL, null);
        AuthContextHolder.set(context);
    }

}
