package com.example.server.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.example.audit.AuditPolicySnapshot;
import com.example.audit.AuditPort;
import com.example.server.config.SensitiveApiProperties;

@DisplayName("SensitiveApiFilter")
class SensitiveApiFilterTest {

    @Test
    @DisplayName("Given 민감 엔드포인트 & 사유 없음 When 호출 Then 400으로 차단한다 (secure-by-default)")
    void rejectWhenReasonMissing() throws ServletException, IOException {
        AuditPort auditPort = Mockito.mock(AuditPort.class);
        when(auditPort.resolve(anyString(), anyString()))
                .thenReturn(Optional.of(AuditPolicySnapshot.builder()
                        .sensitiveApi(true)
                        .reasonRequired(true)
                        .attribute("sensitiveEndpoints", List.of("/api/customers/**"))
                        .build()));

        SensitiveApiProperties props = new SensitiveApiProperties();
        props.setEndpoints(List.of("/api/customers/**"));

        SensitiveApiFilter filter = new SensitiveApiFilter(auditPort, props);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/customers/1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        verify(chain, never()).doFilter(req, res);
    }

    @Test
    @DisplayName("Given 민감 엔드포인트 & 사유/법적근거 존재 When 호출 Then 마스킹타겟 주입 후 체인을 진행한다")
    void allowWhenReasonProvided() throws ServletException, IOException {
        AuditPort auditPort = Mockito.mock(AuditPort.class);
        when(auditPort.resolve(anyString(), anyString()))
                .thenReturn(Optional.of(AuditPolicySnapshot.builder()
                        .sensitiveApi(true)
                        .reasonRequired(true)
                        .attribute("sensitiveEndpoints", List.of("/api/customers/**"))
                        .build()));

        SensitiveApiProperties props = new SensitiveApiProperties();
        props.setEndpoints(List.of("/api/customers/**"));

        SensitiveApiFilter filter = new SensitiveApiFilter(auditPort, props);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/customers/1");
        req.addParameter("reasonCode", "CS01");
        req.addParameter("legalBasisCode", "PIPA");
        MockHttpServletResponse res = new MockHttpServletResponse();

        final boolean[] chainCalled = {false};
        FilterChain chain = (request, response) -> {
            chainCalled[0] = true;
            assertThat(request.getAttribute("AUDIT_MASKING_TARGET")).isNotNull();
        };

        filter.doFilter(req, res, chain);

        assertThat(chainCalled[0]).isTrue();
        assertThat(res.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("Given 정책이 민감 아님 When 호출 Then 검증 없이 통과한다")
    void skipWhenPolicySaysNotSensitive() throws ServletException, IOException {
        AuditPort auditPort = Mockito.mock(AuditPort.class);
        when(auditPort.resolve(anyString(), anyString()))
                .thenReturn(Optional.of(AuditPolicySnapshot.builder()
                        .sensitiveApi(false)
                        .reasonRequired(false)
                        .build()));

        SensitiveApiProperties props = new SensitiveApiProperties();
        props.setEndpoints(List.of("/api/other/**"));

        SensitiveApiFilter filter = new SensitiveApiFilter(auditPort, props);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/health");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = (request, response) -> {
            // 민감하지 않으므로 마스킹 타겟 미설정
            assertThat(request.getAttribute("AUDIT_MASKING_TARGET")).isNull();
        };

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("Given 헤더로 SENSITIVE 표시 & reasonRequired=false When 호출 Then 검증 없이 통과하고 마스킹타겟만 설정한다")
    void headerSensitiveButReasonNotRequired() throws ServletException, IOException {
        AuditPort auditPort = Mockito.mock(AuditPort.class);
        when(auditPort.resolve(anyString(), anyString()))
                .thenReturn(Optional.of(AuditPolicySnapshot.builder()
                        .sensitiveApi(true)
                        .reasonRequired(false)
                        .build()));

        SensitiveApiProperties props = new SensitiveApiProperties();
        SensitiveApiFilter filter = new SensitiveApiFilter(auditPort, props);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/any");
        req.addHeader("X-SENSITIVE-API", "true");
        MockHttpServletResponse res = new MockHttpServletResponse();

        final boolean[] called = {false};
        FilterChain chain = (request, response) -> {
            called[0] = true;
            assertThat(request.getAttribute("AUDIT_MASKING_TARGET")).isNotNull();
        };

        filter.doFilter(req, res, chain);

        assertThat(called[0]).isTrue();
        assertThat(res.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("Given 정책 엔드포인트 매칭 When 호출 Then reason/법적근거 검증 후 마스킹타겟을 설정한다")
    void policyEndpointMatchTriggersValidation() throws ServletException, IOException {
        AuditPort auditPort = Mockito.mock(AuditPort.class);
        when(auditPort.resolve(anyString(), anyString()))
                .thenReturn(Optional.of(AuditPolicySnapshot.builder()
                        .sensitiveApi(true)
                        .reasonRequired(true)
                        .attribute("sensitiveEndpoints", List.of("/secure/**"))
                        .build()));

        SensitiveApiProperties props = new SensitiveApiProperties(); // 빈 endpoints
        SensitiveApiFilter filter = new SensitiveApiFilter(auditPort, props);

        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/secure/data");
        req.addParameter("reasonCode", "RC");
        req.addParameter("legalBasisCode", "LB");
        MockHttpServletResponse res = new MockHttpServletResponse();

        final boolean[] called = {false};
        FilterChain chain = (request, response) -> {
            called[0] = true;
            assertThat(request.getAttribute("AUDIT_MASKING_TARGET")).isNotNull();
        };

        filter.doFilter(req, res, chain);

        assertThat(called[0]).isTrue();
        assertThat(res.getStatus()).isEqualTo(HttpStatus.OK.value());
    }
}
