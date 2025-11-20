package com.example.dwgateway.client;

import java.util.Collections;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.common.security.RowScope;
import com.example.dwgateway.client.dto.OrganizationRecordResponse;
import com.example.dwgateway.dw.DwOrganizationPort;

import org.springframework.retry.support.RetryTemplate;

public class DwOrganizationPortClient implements DwOrganizationPort {

    private static final ParameterizedTypeReference<List<OrganizationRecordResponse>> LIST_TYPE =
            new ParameterizedTypeReference<>() { };

    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;

    public DwOrganizationPortClient(RestTemplate restTemplate, RetryTemplate retryTemplate) {
        this.restTemplate = restTemplate;
        this.retryTemplate = retryTemplate;
    }

    @Override
    public Page<DwOrganizationRecord> getOrganizations(Pageable pageable, RowScope rowScope, String organizationCode) {
        if (rowScope == null) {
            throw new IllegalArgumentException("rowScope must not be null");
        }
        try {
            String path = UriComponentsBuilder.fromPath("/api/dw/organizations")
                    .queryParam("page", pageable.getPageNumber())
                    .queryParam("size", pageable.getPageSize())
                    .queryParam("organizationCode", organizationCode)
                    .queryParam("rowScope", rowScope.name())
                    .build()
                    .toUriString();
            ResponseEntity<List<OrganizationRecordResponse>> response = retryTemplate.execute(context ->
                    restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(null), LIST_TYPE));
            List<OrganizationRecordResponse> body = response.getBody() != null ? response.getBody() : Collections.emptyList();
            List<DwOrganizationRecord> records = body.stream().map(this::toRecord).toList();
            return new PageImpl<>(records, pageable, records.size());
        }
        catch (RestClientException ex) {
            throw new DwGatewayClientException("Failed to fetch organizations", ex);
        }
    }

    private DwOrganizationRecord toRecord(OrganizationRecordResponse response) {
        return new DwOrganizationRecord(response.id(), response.organizationCode(), response.version(),
                response.name(), response.parentOrganizationCode(), response.status(),
                response.effectiveStart(), response.effectiveEnd(), response.syncedAt());
    }
}
