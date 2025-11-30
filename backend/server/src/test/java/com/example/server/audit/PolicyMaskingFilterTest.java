package com.example.server.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.masking.DataKind;
import com.example.common.masking.MaskingContextHolder;
import com.example.common.masking.MaskingTarget;
import com.example.common.masking.SubjectType;
import com.example.common.policy.MaskingPolicyProvider;
import com.example.common.policy.RowAccessMatch;
import com.example.common.policy.RowAccessPolicyProvider;
import com.example.common.security.RowScope;
import com.example.common.security.RowScopeContextHolder;

@DisplayName("PolicyMaskingFilter")
class PolicyMaskingFilterTest {

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
        RowScopeContextHolder.clear();
        MaskingContextHolder.clear();
    }

    @Test
    @DisplayName("Given 인증 컨텍스트 없음 When 필터 실행 Then 정책 평가·마스킹을 건너뛴다")
    void skipWhenNoAuthContext() throws ServletException, IOException {
        RowAccessPolicyProvider provider = Mockito.mock(RowAccessPolicyProvider.class);
        MaskingPolicyProvider maskingProvider = Mockito.mock(MaskingPolicyProvider.class);
        PolicyMaskingFilter filter = new PolicyMaskingFilter(provider, maskingProvider);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(provider, never()).evaluate(any());
        assertThat(MaskingContextHolder.get()).isNull();
        assertThat(RowScopeContextHolder.get()).isNull();
    }

    @Test
    @DisplayName("Given 정책 매치 발생 When 필터 실행 Then RowScope/Masking 컨텍스트를 설정하고 종료 시 정리한다")
    void applyWhenPolicyMatches() throws ServletException, IOException {
        RowAccessPolicyProvider provider = Mockito.mock(RowAccessPolicyProvider.class);
        when(provider.evaluate(any())).thenReturn(Optional.of(
                RowAccessMatch.builder()
                        .policyId(UUID.randomUUID())
                        .rowScope(RowScope.ORG)
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

        MaskingPolicyProvider maskingProvider = Mockito.mock(MaskingPolicyProvider.class);
        PolicyMaskingFilter filter = new PolicyMaskingFilter(provider, maskingProvider);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customers");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (request1, response1) -> {
            assertThat(request1.getAttribute(PolicyMaskingFilter.ATTR_ROW_ACCESS_MATCH)).isNotNull();
            assertThat(RowScopeContextHolder.get()).isNotNull();
            MaskingTarget target = (MaskingTarget) request1.getAttribute(PolicyMaskingFilter.ATTR_MASKING_TARGET);
            assertThat(target).isNotNull();
            assertThat(target.getSubjectType()).isEqualTo(SubjectType.UNKNOWN);
        };

        filter.doFilter(request, response, chain);

        assertThat(MaskingContextHolder.get()).isNull();
        assertThat(RowScopeContextHolder.get()).isNull();
    }

    @Test
    @DisplayName("Given 정책 미매치 When 필터 실행 Then RowScope 컨텍스트를 설정하지 않고 MaskingTarget만 설정한다")
    void skipRowScopeWhenPolicyNotMatched() throws ServletException, IOException {
        RowAccessPolicyProvider provider = Mockito.mock(RowAccessPolicyProvider.class);
        when(provider.evaluate(any())).thenReturn(Optional.empty());

        AuthContextHolder.set(AuthContext.of(
                "user2",
                null,
                "AUDIT_VIEWER",
                FeatureCode.AUDIT_LOG,
                ActionCode.READ,
                null
        ));

        MaskingPolicyProvider maskingProvider = Mockito.mock(MaskingPolicyProvider.class);
        PolicyMaskingFilter filter = new PolicyMaskingFilter(provider, maskingProvider);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customers");
        MockHttpServletResponse response = new MockHttpServletResponse();
        final boolean[] checked = {false};
        FilterChain chain = (req, res) -> {
            assertThat(req.getAttribute(PolicyMaskingFilter.ATTR_ROW_ACCESS_MATCH)).isNull();
            assertThat(RowScopeContextHolder.get()).isNull();
            // MaskingTarget은 항상 설정됨
            assertThat(req.getAttribute(PolicyMaskingFilter.ATTR_MASKING_TARGET)).isNotNull();
            checked[0] = true;
        };

        filter.doFilter(request, response, chain);

        verify(provider).evaluate(any());
        assertThat(checked[0]).isTrue();
        assertThat(MaskingContextHolder.get()).isNull();
        assertThat(RowScopeContextHolder.get()).isNull();
    }

    @Test
    @DisplayName("Given 기존 MaskingTarget 존재 When 정책 매치 Then 기존 설정을 유지하면서 병합한다")
    void mergeExistingMaskingTarget() throws ServletException, IOException {
        RowAccessPolicyProvider provider = Mockito.mock(RowAccessPolicyProvider.class);
        when(provider.evaluate(any())).thenReturn(Optional.of(
                RowAccessMatch.builder()
                        .policyId(UUID.randomUUID())
                        .rowScope(RowScope.ALL)
                        .priority(2)
                        .build()
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
                .dataKind(DataKind.DEFAULT)
                .defaultMask(false)
                .forceUnmask(true)
                .forceUnmaskKinds(new java.util.HashSet<>(java.util.Set.of(DataKind.DEFAULT)))
                .forceUnmaskFields(new java.util.HashSet<>(List.of("name")))
                .requesterRoles(new java.util.HashSet<>(List.of("AUDIT_VIEWER")))
                .rowId("ROW-1")
                .build();

        MaskingPolicyProvider maskingProvider = Mockito.mock(MaskingPolicyProvider.class);
        PolicyMaskingFilter filter = new PolicyMaskingFilter(provider, maskingProvider);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customers/1");
        request.setAttribute(PolicyMaskingFilter.ATTR_MASKING_TARGET, base);
        MockHttpServletResponse response = new MockHttpServletResponse();

        final boolean[] asserted = {false};
        FilterChain chain = (req, res) -> {
            MaskingTarget merged = (MaskingTarget) req.getAttribute(PolicyMaskingFilter.ATTR_MASKING_TARGET);
            assertThat(merged.getSubjectType()).isEqualTo(SubjectType.CUSTOMER_INDIVIDUAL);
            assertThat(merged.isForceUnmask()).isTrue();
            assertThat(merged.getForceUnmaskFields()).contains("name");
            asserted[0] = true;
        };

        filter.doFilter(request, response, chain);

        assertThat(asserted[0]).isTrue();
        assertThat(MaskingContextHolder.get()).isNull();
    }
}
