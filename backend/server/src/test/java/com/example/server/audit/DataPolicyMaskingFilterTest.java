package com.example.server.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.masking.MaskingContextHolder;
import com.example.common.masking.MaskingTarget;
import com.example.common.masking.SubjectType;
import com.example.common.policy.DataPolicyContextHolder;
import com.example.common.policy.DataPolicyMatch;
import com.example.common.policy.DataPolicyProvider;
import com.example.common.security.RowScopeContextHolder;

@DisplayName("DataPolicyMaskingFilter")
class DataPolicyMaskingFilterTest {

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
        DataPolicyContextHolder.clear();
        RowScopeContextHolder.clear();
        MaskingContextHolder.clear();
    }

    @Test
    @DisplayName("Given 인증 컨텍스트 없음 When 필터 실행 Then 정책 평가·마스킹을 건너뛴다")
    void skipWhenNoAuthContext() throws ServletException, IOException {
        DataPolicyProvider provider = Mockito.mock(DataPolicyProvider.class);
        DataPolicyMaskingFilter filter = new DataPolicyMaskingFilter(provider);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(provider, never()).evaluate(any());
        assertThat(DataPolicyContextHolder.get()).isNull();
        assertThat(MaskingContextHolder.get()).isNull();
        assertThat(RowScopeContextHolder.get()).isNull();
    }

    @Test
    @DisplayName("Given 정책 매치 발생 When 필터 실행 Then RowScope/Masking/DataPolicy 컨텍스트를 설정하고 종료 시 정리한다")
    void applyWhenPolicyMatches() throws ServletException, IOException {
        DataPolicyProvider provider = Mockito.mock(DataPolicyProvider.class);
        when(provider.evaluate(any())).thenReturn(Optional.of(
                DataPolicyMatch.builder()
                        .maskRule("FULL")
                        .maskParams("NAME")
                        .priority(1)
                        .build()
        ));

        AuthContextHolder.set(AuthContext.of(
                "user1",
                "ORG1",
                "AUDIT_VIEWER",
                FeatureCode.AUDIT_LOG,
                ActionCode.READ,
                null
        ));

        DataPolicyMaskingFilter filter = new DataPolicyMaskingFilter(provider);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customers");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (request1, response1) -> {
            assertThat(request1.getAttribute(DataPolicyMaskingFilter.ATTR_POLICY_MATCH)).isNotNull();
            assertThat(DataPolicyContextHolder.get()).isNotNull();
            assertThat(RowScopeContextHolder.get()).isNotNull();
            MaskingTarget target = (MaskingTarget) request1.getAttribute(DataPolicyMaskingFilter.ATTR_MASKING_TARGET);
            assertThat(target).isNotNull();
            assertThat(target.getMaskRule()).isEqualTo("FULL");
            assertThat(target.getSubjectType()).isEqualTo(SubjectType.UNKNOWN);
        };

        filter.doFilter(request, response, chain);

        assertThat(DataPolicyContextHolder.get()).isNull();
        assertThat(MaskingContextHolder.get()).isNull();
        assertThat(RowScopeContextHolder.get()).isNull();
    }

    @Test
    @DisplayName("Given 정책 미매치 When 필터 실행 Then 어떤 컨텍스트도 설정하지 않는다")
    void skipWhenPolicyNotMatched() throws ServletException, IOException {
        DataPolicyProvider provider = Mockito.mock(DataPolicyProvider.class);
        when(provider.evaluate(any())).thenReturn(Optional.empty());

        AuthContextHolder.set(AuthContext.of(
                "user2",
                null,
                "AUDIT_VIEWER",
                FeatureCode.AUDIT_LOG,
                ActionCode.READ,
                null
        ));

        DataPolicyMaskingFilter filter = new DataPolicyMaskingFilter(provider);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customers");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(provider).evaluate(any());
        assertThat(request.getAttribute(DataPolicyMaskingFilter.ATTR_POLICY_MATCH)).isNull();
        assertThat(MaskingContextHolder.get()).isNull();
        assertThat(RowScopeContextHolder.get()).isNull();
    }

    @Test
    @DisplayName("Given 기존 MaskingTarget 존재 When 정책 매치 Then maskRule만 덮어씌워 병합한다")
    void mergeExistingMaskingTarget() throws ServletException, IOException {
        DataPolicyProvider provider = Mockito.mock(DataPolicyProvider.class);
        when(provider.evaluate(any())).thenReturn(Optional.of(
                DataPolicyMatch.builder().maskRule("HASH").maskParams("name").priority(2).build()
        ));

        AuthContextHolder.set(AuthContext.of(
                "auditor",
                "ORG1",
                "AUDIT_VIEWER",
                FeatureCode.AUDIT_LOG,
                ActionCode.READ,
                null
        ));

        MaskingTarget base = MaskingTarget.builder()
                .subjectType(SubjectType.CUSTOMER_INDIVIDUAL)
                .dataKind("HTTP")
                .defaultMask(false)
                .forceUnmask(true)
                .forceUnmaskKinds(new java.util.HashSet<>(List.of("HTTP")))
                .forceUnmaskFields(new java.util.HashSet<>(List.of("name")))
                .requesterRoles(new java.util.HashSet<>(List.of("AUDIT_VIEWER")))
                .rowId("ROW-1")
                .build();

        DataPolicyMaskingFilter filter = new DataPolicyMaskingFilter(provider);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customers/1");
        request.setAttribute(DataPolicyMaskingFilter.ATTR_MASKING_TARGET, base);
        MockHttpServletResponse response = new MockHttpServletResponse();

        final boolean[] asserted = {false};
        FilterChain chain = (req, res) -> {
            MaskingTarget merged = (MaskingTarget) req.getAttribute(DataPolicyMaskingFilter.ATTR_MASKING_TARGET);
            assertThat(merged.getSubjectType()).isEqualTo(SubjectType.CUSTOMER_INDIVIDUAL);
            assertThat(merged.isForceUnmask()).isTrue();
            assertThat(merged.getForceUnmaskFields()).contains("name");
            assertThat(merged.getMaskRule()).isEqualTo("HASH");
            asserted[0] = true;
        };

        filter.doFilter(request, response, chain);

        assertThat(asserted[0]).isTrue();
        assertThat(MaskingContextHolder.get()).isNull();
    }
}
