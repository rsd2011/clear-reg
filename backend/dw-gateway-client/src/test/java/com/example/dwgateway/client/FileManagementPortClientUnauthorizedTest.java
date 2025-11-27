package com.example.dwgateway.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import com.example.common.file.dto.FileMetadataDto;

class FileManagementPortClientUnauthorizedTest {

    RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
    RetryTemplate retryTemplate = RetryTemplate.builder().maxAttempts(1).build();

    @Test
    @DisplayName("다운로드 메타데이터 조회가 401이면 DwGatewayClientException이 발생한다")
    void download_unauthorized_propagates() {
        FileManagementPortClient client = new FileManagementPortClient(restTemplate, retryTemplate);
        RestClientResponseException unauthorized = new RestClientResponseException("401", 401, "UNAUTHORIZED", null, null, null);
        given(restTemplate.getForObject(Mockito.anyString(), Mockito.eq(FileMetadataDto.class)))
                .willThrow(unauthorized);

        assertThatThrownBy(() -> client.download(UUID.randomUUID(), "token", java.util.List.of()))
                .isInstanceOf(DwGatewayClientException.class)
                .hasCauseInstanceOf(RestClientResponseException.class);
    }

    @Test
    @DisplayName("파일 목록 조회 실패 시 DwGatewayClientException으로 래핑된다")
    void list_failure_wraps() {
        FileManagementPortClient client = new FileManagementPortClient(restTemplate, retryTemplate);
        given(restTemplate.exchange(Mockito.anyString(), Mockito.eq(org.springframework.http.HttpMethod.GET), Mockito.any(), Mockito.<org.springframework.core.ParameterizedTypeReference<java.util.List<FileMetadataDto>>>any()))
                .willThrow(new RestClientResponseException("500", 500, "ERR", null, null, null));

        assertThatThrownBy(client::list)
                .isInstanceOf(DwGatewayClientException.class);
    }
}
