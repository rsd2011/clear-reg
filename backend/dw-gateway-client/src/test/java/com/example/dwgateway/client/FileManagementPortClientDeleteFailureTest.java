package com.example.dwgateway.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.common.file.dto.FileMetadataDto;

class FileManagementPortClientDeleteFailureTest {

    RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
    RetryTemplate retryTemplate = RetryTemplate.builder().maxAttempts(1).build();

    @Test
    @DisplayName("삭제 중 RestClientException이 발생하면 DwGatewayClientException으로 래핑된다")
    void delete_restClientException_wraps() {
        FileManagementPortClient client = new FileManagementPortClient(restTemplate, retryTemplate);
        given(restTemplate.exchange(Mockito.anyString(), Mockito.eq(HttpMethod.DELETE), Mockito.<HttpEntity<?>>any(), Mockito.<Class<FileMetadataDto>>any()))
                .willThrow(new RestClientException("delete-fail"));

        assertThatThrownBy(() -> client.delete(UUID.randomUUID(), "actor"))
                .isInstanceOf(DwGatewayClientException.class);
    }
}
