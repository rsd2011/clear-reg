package com.example.file.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.file.api.dto.FileUploadRequest;

@DisplayName("FileUploadRequest 테스트")
class FileUploadRequestTest {

    @Test
    @DisplayName("Given 보존 기한 When 레코드 생성하면 Then 동일 값이 유지된다")
    void retainsRetentionUntil() {
        OffsetDateTime retentionUntil = OffsetDateTime.now().plusDays(7);

        FileUploadRequest request = new FileUploadRequest(retentionUntil);

        assertThat(request.retentionUntil()).isEqualTo(retentionUntil);
    }
}
