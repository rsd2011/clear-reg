package com.example.dwgateway.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;
import org.springframework.retry.support.RetryTemplate;

import com.example.dw.application.policy.DwBatchJobScheduleRequest;
import com.example.dw.application.policy.DwBatchJobScheduleView;
import com.example.dw.application.policy.DwIngestionPolicyUpdateRequest;
import com.example.dw.application.policy.DwIngestionPolicyView;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

class DwIngestionPolicyPortClientTest {

    private MockRestServiceServer server;
    private DwIngestionPolicyPortClient client;
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        this.restTemplate = new RestTemplateBuilder()
                .rootUri("http://localhost")
                .build();
        this.server = MockRestServiceServer.bindTo(this.restTemplate).ignoreExpectOrder(true).build();
        this.client = new DwIngestionPolicyPortClient(this.restTemplate,
                RetryTemplate.builder().maxAttempts(1).fixedBackoff(10).build());
    }

    @Test
    void currentPolicyReturnsBody() {
        server.expect(requestTo("http://localhost/api/admin/dw-ingestion/policy"))
                .andRespond(MockRestResponseCreators.withSuccess("""
                        {
                          "batchCron": "0 0 * * *",
                          "timezone": "UTC",
                          "retention": "PT24H",
                          "jobSchedules": [
                            {
                              "jobKey": "hr-import",
                              "enabled": true,
                              "cronExpression": "0 0 * * *",
                              "timezone": "UTC"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        DwIngestionPolicyView view = client.currentPolicy();

        assertThat(view.batchCron()).isEqualTo("0 0 * * *");
        assertThat(view.jobSchedules()).hasSize(1);
        assertThat(view.jobSchedules().getFirst().jobKey()).isEqualTo("hr-import");
    }

    @Test
    void currentPolicyThrowsWhenBodyMissing() {
        server.expect(requestTo("http://localhost/api/admin/dw-ingestion/policy"))
                .andRespond(MockRestResponseCreators.withSuccess());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> client.currentPolicy())
                .isInstanceOf(DwGatewayClientException.class)
                .hasMessageContaining("response body");
    }

    @Test
    void updatePolicySendsRequestBody() {
        server.expect(requestTo("http://localhost/api/admin/dw-ingestion/policy"))
                .andExpect(method(org.springframework.http.HttpMethod.PUT))
                .andRespond(MockRestResponseCreators.withSuccess("""
                        {"batchCron":"0 0 12 * *","timezone":"UTC","retention":"PT12H","jobSchedules":[]}
                        """, MediaType.APPLICATION_JSON));

        DwIngestionPolicyUpdateRequest request = new DwIngestionPolicyUpdateRequest(
                "0 0 12 * *",
                "UTC",
                Duration.ofHours(12),
                List.of(new DwBatchJobScheduleRequest("hr-import", true, "0 0 12 * *", "UTC")));

        DwIngestionPolicyView view = client.updatePolicy(request);

        assertThat(view.retention()).isEqualTo(Duration.ofHours(12));
    }

    @Test
    void updatePolicyRetriesOnServerError() {
        server.expect(requestTo("http://localhost/api/admin/dw-ingestion/policy"))
                .andExpect(method(org.springframework.http.HttpMethod.PUT))
                .andRespond(MockRestResponseCreators.withServerError());
        server.expect(requestTo("http://localhost/api/admin/dw-ingestion/policy"))
                .andExpect(method(org.springframework.http.HttpMethod.PUT))
                .andRespond(MockRestResponseCreators.withSuccess("""
                        {"batchCron":"0 0 12 * *","timezone":"UTC","retention":"PT12H","jobSchedules":[]}
                        """, MediaType.APPLICATION_JSON));

        DwIngestionPolicyUpdateRequest request = new DwIngestionPolicyUpdateRequest(
                "0 0 12 * *",
                "UTC",
                Duration.ofHours(12),
                List.of(new DwBatchJobScheduleRequest("hr-import", true, "0 0 12 * *", "UTC")));
        DwIngestionPolicyPortClient retryingClient = new DwIngestionPolicyPortClient(
                restTemplate, RetryTemplate.builder().maxAttempts(2).fixedBackoff(10).build());

        DwIngestionPolicyView view = retryingClient.updatePolicy(request);
        assertThat(view.batchCron()).isEqualTo("0 0 12 * *");
    }

    @Test
    void updatePolicyThrowsWhenBodyMissing() {
        server.expect(requestTo("http://localhost/api/admin/dw-ingestion/policy"))
                .andExpect(method(org.springframework.http.HttpMethod.PUT))
                .andRespond(MockRestResponseCreators.withSuccess());

        DwIngestionPolicyUpdateRequest request = new DwIngestionPolicyUpdateRequest(
                "0 0 12 * *",
                "UTC",
                Duration.ofHours(12),
                List.of());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> client.updatePolicy(request))
                .isInstanceOf(DwGatewayClientException.class)
                .hasMessageContaining("response body");
    }
}
