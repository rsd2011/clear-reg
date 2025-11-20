package com.example.dwgateway.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;
import org.springframework.retry.support.RetryTemplate;

import com.example.dw.domain.HrBatchStatus;
import com.example.dw.dto.DataFeedType;
import com.example.dwgateway.dw.DwBatchPort;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

class DwBatchPortClientTest {

    private MockRestServiceServer server;
    private DwBatchPort client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplateBuilder()
                .rootUri("http://localhost")
                .build();
        this.server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
        this.client = new DwBatchPortClient(restTemplate,
                RetryTemplate.builder().maxAttempts(1).fixedBackoff(10).build());
    }

    @Test
    void getBatchesReturnsPage() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo("http://localhost/api/dw/batches"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(MockRestResponseCreators.withSuccess("""
                        [
                          {
                            "id": "%s",
                            "fileName": "hr.csv",
                            "feedType": "%s",
                            "sourceName": "hr-system",
                            "businessDate": "2024-01-01",
                            "status": "%s",
                            "totalRecords": 10,
                            "insertedRecords": 10,
                            "updatedRecords": 0,
                            "failedRecords": 0,
                            "receivedAt": "2024-01-01T00:00:00Z",
                            "completedAt": "2024-01-01T00:05:00Z",
                            "errorMessage": null
                          }
                        ]
                        """.formatted(id, DataFeedType.EMPLOYEE.name(), HrBatchStatus.COMPLETED.name()), MediaType.APPLICATION_JSON));

        Page<DwBatchPort.DwBatchRecord> page = client.getBatches(Pageable.unpaged());

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().id()).isEqualTo(id);
    }

    @Test
    void latestBatchEmptyWhenNotFound() {
        server.expect(requestTo("http://localhost/api/dw/batches/latest"))
                .andRespond(MockRestResponseCreators.withStatus(org.springframework.http.HttpStatus.NOT_FOUND));

        assertThat(client.latestBatch()).isEmpty();
    }

    @Test
    void latestBatchReturnsBody() {
        server.expect(requestTo("http://localhost/api/dw/batches/latest"))
                .andRespond(MockRestResponseCreators.withSuccess("""
                        {
                          "id": "%s",
                          "fileName": "hr.csv",
                          "feedType": "%s",
                          "sourceName": "hr-system",
                          "businessDate": "2024-01-01",
                          "status": "%s",
                          "totalRecords": 10,
                          "insertedRecords": 10,
                          "updatedRecords": 0,
                          "failedRecords": 0,
                          "receivedAt": "2024-01-01T00:00:00Z",
                          "completedAt": "2024-01-01T00:05:00Z",
                          "errorMessage": null
                        }
                        """.formatted(UUID.randomUUID(), DataFeedType.EMPLOYEE.name(), HrBatchStatus.COMPLETED.name()), MediaType.APPLICATION_JSON));

        assertThat(client.latestBatch()).isPresent();
    }
}
