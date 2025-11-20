package com.example.dwgateway.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;
import org.springframework.retry.support.RetryTemplate;

import com.example.common.security.RowScope;
import com.example.dwgateway.dw.DwOrganizationPort;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

class DwOrganizationPortClientTest {

    private MockRestServiceServer server;
    private RestTemplate restTemplate;
    private DwOrganizationPort client;

    @BeforeEach
    void setUp() {
        this.restTemplate = new RestTemplateBuilder()
                .rootUri("http://localhost")
                .build();
        this.server = MockRestServiceServer.bindTo(this.restTemplate).ignoreExpectOrder(true).build();
        this.client = new DwOrganizationPortClient(this.restTemplate,
                RetryTemplate.builder().maxAttempts(1).fixedBackoff(10).build());
    }

    @Test
    void getOrganizationsReturnsPage() {
        String responseBody = """
                [
                  {
                    "id": "%s",
                    "organizationCode": "ORG001",
                    "version": 1,
                    "name": "HQ",
                    "parentOrganizationCode": null,
                    "status": "ACTIVE",
                    "effectiveStart": "2024-01-01",
                    "effectiveEnd": null,
                    "syncedAt": "2024-01-02T00:00:00Z"
                  }
                ]
                """.formatted(UUID.randomUUID());
        server.expect(requestTo("http://localhost/api/dw/organizations?page=0&size=20&organizationCode=ORG001&rowScope=ALL"))
                .andRespond(MockRestResponseCreators.withSuccess(responseBody, MediaType.APPLICATION_JSON));

        Page<DwOrganizationPort.DwOrganizationRecord> page = client.getOrganizations(
                PageRequest.of(0, 20), RowScope.ALL, "ORG001");

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().name()).isEqualTo("HQ");
    }
}
