package com.example.platform.file;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.file.FileMetadataDto;
import com.example.common.file.FileStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

@DisplayName("파일 DTO 값 보존")
class FileDtoValueTest {

    @Test
    @DisplayName("FileMetadataDto 필드가 그대로 보존된다")
    void metadataPreservesValues() {
        OffsetDateTime now = OffsetDateTime.now();
        FileMetadataDto dto = new FileMetadataDto(UUID.randomUUID(), "name", "text/plain", 10L,
                "hash", "owner", FileStatus.ACTIVE, now.plusDays(1), now, now);

        assertThat(dto.originalName()).isEqualTo("name");
        assertThat(dto.retentionUntil()).isEqualTo(now.plusDays(1));
    }
}
