package com.example.dwgateway.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

class FileManagementPortClientFailureTest {

    RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
    RetryTemplate retryTemplate = RetryTemplate.builder().maxAttempts(1).build();

    @Test
    @DisplayName("다운로드가 500이면 RuntimeException으로 전환된다")
    void download_serverError_throws() {
        FileManagementPortClient client = new FileManagementPortClient(restTemplate, retryTemplate);
        given(restTemplate.exchange(Mockito.anyString(), Mockito.eq(org.springframework.http.HttpMethod.GET), Mockito.any(), Mockito.eq(byte[].class)))
                .willThrow(new RuntimeException("500"));

        assertThatThrownBy(() -> client.download(UUID.randomUUID(), "token", java.util.List.of()))
                .isInstanceOf(RuntimeException.class);
    }
}
