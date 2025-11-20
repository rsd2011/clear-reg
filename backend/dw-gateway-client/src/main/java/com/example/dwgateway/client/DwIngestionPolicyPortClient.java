package com.example.dwgateway.client;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.dw.application.policy.DwIngestionPolicyUpdateRequest;
import com.example.dw.application.policy.DwIngestionPolicyView;
import com.example.dwgateway.dw.DwIngestionPolicyPort;

import org.springframework.retry.support.RetryTemplate;

/**
 * HTTP client implementation of {@link DwIngestionPolicyPort} that targets the dw-gateway service.
 */
public class DwIngestionPolicyPortClient implements DwIngestionPolicyPort {

    private static final String POLICY_PATH = "/api/admin/dw-ingestion/policy";

    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;

    public DwIngestionPolicyPortClient(RestTemplate restTemplate, RetryTemplate retryTemplate) {
        this.restTemplate = restTemplate;
        this.retryTemplate = retryTemplate;
    }

    @Override
    public DwIngestionPolicyView currentPolicy() {
        try {
            DwIngestionPolicyView body = retryTemplate.execute(context ->
                    restTemplate.getForObject(POLICY_PATH, DwIngestionPolicyView.class));
            if (body == null) {
                throw new DwGatewayClientException("DW ingestion policy response body was empty");
            }
            return body;
        }
        catch (RestClientException ex) {
            throw new DwGatewayClientException("Failed to fetch DW ingestion policy", ex);
        }
    }

    @Override
    public DwIngestionPolicyView updatePolicy(DwIngestionPolicyUpdateRequest request) {
        try {
            HttpEntity<DwIngestionPolicyUpdateRequest> entity = new HttpEntity<>(request);
            ResponseEntity<DwIngestionPolicyView> response = retryTemplate.execute(context ->
                    restTemplate.exchange(POLICY_PATH, HttpMethod.PUT, entity, DwIngestionPolicyView.class));
            DwIngestionPolicyView body = response.getBody();
            if (body == null) {
                throw new DwGatewayClientException("DW ingestion policy update response body was empty");
            }
            return body;
        }
        catch (RestClientException ex) {
            throw new DwGatewayClientException("Failed to update DW ingestion policy", ex);
        }
    }
}
