package com.example.dwgateway.client;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.dwgateway.dw.DwBatchPort;

import org.springframework.retry.support.RetryTemplate;

/**
 * HTTP client implementation of {@link DwBatchPort} that targets the dw-gateway service.
 */
public class DwBatchPortClient implements DwBatchPort {

    private static final String BATCHES_PATH = "/api/dw/batches";

    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;

    public DwBatchPortClient(RestTemplate restTemplate, RetryTemplate retryTemplate) {
        this.restTemplate = restTemplate;
        this.retryTemplate = retryTemplate;
    }

    @Override
    public Page<DwBatchRecord> getBatches(Pageable pageable) {
        try {
            ResponseEntity<List<DwBatchRecord>> response = retryTemplate.execute(context ->
                    restTemplate.exchange(BATCHES_PATH,
                            org.springframework.http.HttpMethod.GET, null,
                            new ParameterizedTypeReference<List<DwBatchRecord>>() { }));
            List<DwBatchRecord> records = response.getBody() != null ? response.getBody() : Collections.emptyList();
            return new PageImpl<>(records, pageable, records.size());
        }
        catch (RestClientException ex) {
            throw new DwGatewayClientException("Failed to fetch DW batches", ex);
        }
    }

    @Override
    public Optional<DwBatchRecord> latestBatch() {
        try {
            DwBatchRecord record = retryTemplate.execute(context ->
                    restTemplate.getForObject(BATCHES_PATH + "/latest", DwBatchRecord.class));
            return Optional.ofNullable(record);
        }
        catch (HttpClientErrorException.NotFound notFound) {
            return Optional.empty();
        }
        catch (RestClientException ex) {
            throw new DwGatewayClientException("Failed to read DW latest batch", ex);
        }
    }
}
