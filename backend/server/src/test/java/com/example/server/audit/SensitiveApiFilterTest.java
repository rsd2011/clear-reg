package com.example.server.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.example.audit.AuditPolicySnapshot;
import com.example.audit.AuditPort;
import com.example.server.config.SensitiveApiProperties;

class SensitiveApiFilterTest {

    @Test
    void deniesSensitiveApiWithoutReason() throws ServletException, IOException {
        AuditPort auditPort = Mockito.mock(AuditPort.class);
        Mockito.when(auditPort.resolve(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(java.util.Optional.of(AuditPolicySnapshot.secureDefault()));

        SensitiveApiProperties props = new SensitiveApiProperties();
        props.setEndpoints(List.of("/api/customers/**"));

        SensitiveApiFilter filter = new SensitiveApiFilter(auditPort, props);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customers/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    void allowsWhenReasonProvided() throws ServletException, IOException {
        AuditPort auditPort = Mockito.mock(AuditPort.class);
        Mockito.when(auditPort.resolve(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(java.util.Optional.of(AuditPolicySnapshot.secureDefault()));

        SensitiveApiProperties props = new SensitiveApiProperties();
        props.setEndpoints(List.of("/api/customers/**"));

        SensitiveApiFilter filter = new SensitiveApiFilter(auditPort, props);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customers/1");
        request.addParameter("reasonCode", "CS01");
        request.addParameter("legalBasisCode", "PIPA");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }
}
