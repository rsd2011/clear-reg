package com.example.dwgateway.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import com.example.common.file.dto.FileMetadataDto;

class FileManagementPortClientDeleteNullTest {

    RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
    RetryTemplate retryTemplate = RetryTemplate.builder().maxAttempts(1).build();

    @Test
    @DisplayName("삭제 응답 body가 null이면 예외를 던진다")
    void delete_nullBody_throws() {
        FileManagementPortClient client = new FileManagementPortClient(restTemplate, retryTemplate);
        given(restTemplate.exchange(Mockito.anyString(), Mockito.eq(org.springframework.http.HttpMethod.DELETE), Mockito.any(), Mockito.<Class<FileMetadataDto>>any()))
                .willReturn(ResponseEntity.ok().body(null));

        assertThatThrownBy(() -> client.delete(UUID.randomUUID(), "actor"))
                .isInstanceOf(DwGatewayClientException.class);
    }
}
