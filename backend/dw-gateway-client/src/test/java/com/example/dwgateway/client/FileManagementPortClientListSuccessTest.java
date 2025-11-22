package com.example.dwgateway.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import com.example.common.file.FileMetadataDto;

class FileManagementPortClientListSuccessTest {

    RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
    RetryTemplate retryTemplate = RetryTemplate.builder().maxAttempts(1).build();

    @Test
    @DisplayName("파일 목록이 정상 반환되면 빈 리스트가 아니다")
    void list_success_returnsItems() {
        FileManagementPortClient client = new FileManagementPortClient(restTemplate, retryTemplate);
        ResponseEntity<List<FileMetadataDto>> response = ResponseEntity.ok(List.of(new FileMetadataDto(
                java.util.UUID.randomUUID(), "f", "text/plain", 1L, "chk", "owner",
                com.example.common.file.FileStatus.ACTIVE, null, null, null)));
        given(restTemplate.exchange(Mockito.anyString(), Mockito.eq(HttpMethod.GET), Mockito.any(HttpEntity.class),
                Mockito.<ParameterizedTypeReference<List<FileMetadataDto>>>any()))
                .willReturn(response);

        List<FileMetadataDto> result = client.list();

        assertThat(result).hasSize(1);
    }
}
