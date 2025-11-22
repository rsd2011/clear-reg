package com.example.dwgateway.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.common.file.FileMetadataDto;
import com.example.file.api.FileUploadRequest;
import com.example.file.FileUploadCommand;

class FileManagementPortClientUploadNullResponseTest {

    RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
    RetryTemplate retryTemplate = RetryTemplate.builder().maxAttempts(1).build();

    @Test
    @DisplayName("업로드 응답이 null이면 DwGatewayClientException을 던진다")
    void upload_nullResponse_throws() {
        FileManagementPortClient client = new FileManagementPortClient(restTemplate, retryTemplate);
        FileUploadCommand command = new FileUploadCommand("n", "text/plain", 1L, () -> new java.io.ByteArrayInputStream("hi".getBytes()), null, "actor");
        given(restTemplate.postForObject(Mockito.anyString(), Mockito.<HttpEntity<?>>any(), Mockito.eq(FileMetadataDto.class)))
                .willReturn(null);

        assertThatThrownBy(() -> client.upload(command))
                .isInstanceOf(DwGatewayClientException.class);
    }
}
