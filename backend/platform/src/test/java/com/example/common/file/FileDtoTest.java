package com.example.common.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import com.example.common.file.dto.FileMetadataDto;

class FileDtoTest {

    @Test
    @DisplayName("FileStatus enum 값이 변환 없이 노출된다")
    void fileStatusValues() {
        assertThat(FileStatus.valueOf("ACTIVE")).isEqualTo(FileStatus.ACTIVE);
        assertThat(FileStatus.valueOf("DELETED")).isEqualTo(FileStatus.DELETED);
    }

    @Test
    @DisplayName("FileMetadataDto는 전달한 값을 그대로 보존한다")
    void fileMetadataDtoStoresValues() {
        OffsetDateTime now = OffsetDateTime.now();
        UUID id = UUID.randomUUID();
        FileMetadataDto dto = new FileMetadataDto(id, "name", "text/plain", 10L, "chk", "owner",
                FileStatus.ACTIVE, now.plusDays(1), now, now);

        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.originalName()).isEqualTo("name");
        assertThat(dto.contentType()).isEqualTo("text/plain");
        assertThat(dto.size()).isEqualTo(10L);
        assertThat(dto.checksum()).isEqualTo("chk");
        assertThat(dto.ownerUsername()).isEqualTo("owner");
        assertThat(dto.status()).isEqualTo(FileStatus.ACTIVE);
        assertThat(dto.retentionUntil()).isEqualTo(now.plusDays(1));
        assertThat(dto.createdAt()).isEqualTo(now);
        assertThat(dto.updatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("FileDownload는 메타데이터와 리소스를 함께 보존한다")
    void fileDownloadHoldsMetadataAndResource() {
        FileMetadataDto metadata = new FileMetadataDto(UUID.randomUUID(), "file", "text/plain", 1L, "c", "u",
                FileStatus.ACTIVE, null, OffsetDateTime.now(), OffsetDateTime.now());
        ByteArrayResource resource = new ByteArrayResource("data".getBytes());

        FileDownload download = new FileDownload(metadata, resource);

        assertThat(download.metadata()).isEqualTo(metadata);
        assertThat(download.resource()).isEqualTo(resource);
    }
}
