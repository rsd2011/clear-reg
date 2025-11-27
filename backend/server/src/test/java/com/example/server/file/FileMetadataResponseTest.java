package com.example.server.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.file.FileMetadataDto;
import com.example.common.file.FileStatus;
import com.example.server.file.dto.FileMetadataResponse;

@DisplayName("FileMetadataResponse 변환/마스킹")
class FileMetadataResponseTest {

    @Test
    @DisplayName("Given masker 미지정 When DTO 변환 Then 원본 필드를 그대로 사용한다")
    void fromDtoDefault() {
        FileMetadataDto dto = new FileMetadataDto(
                UUID.randomUUID(),
                "원본.xlsx",
                "application/vnd.ms-excel",
                10L,
                "abc",
                "owner1",
                FileStatus.ACTIVE,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                OffsetDateTime.now().plusDays(1)
        );

        FileMetadataResponse res = FileMetadataResponse.fromDto(dto);

        assertThat(res.originalName()).isEqualTo(dto.originalName());
        assertThat(res.ownerUsername()).isEqualTo(dto.ownerUsername());
    }

    @Test
    @DisplayName("Given masker 제공 When DTO 변환 Then 파일명/소유자에 마스킹을 적용한다")
    void fromDtoWithMasker() {
        FileMetadataDto dto = new FileMetadataDto(
                UUID.randomUUID(),
                "원본.xlsx",
                "application/vnd.ms-excel",
                10L,
                "abc",
                "owner1",
                FileStatus.ACTIVE,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                OffsetDateTime.now().plusDays(1)
        );

        AtomicInteger called = new AtomicInteger();
        FileMetadataResponse res = FileMetadataResponse.fromDto(dto, v -> {
            called.incrementAndGet();
            return "[MASK]";
        });

        assertThat(res.originalName()).isEqualTo("[MASK]");
        assertThat(res.ownerUsername()).isEqualTo("[MASK]");
        assertThat(called.get()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Given masker=null When DTO 변환 Then 기본 identity가 적용된다")
    void fromDtoNullMaskerUsesIdentity() {
        FileMetadataDto dto = new FileMetadataDto(
                UUID.randomUUID(),
                "report.pdf",
                "application/pdf",
                20L,
                "def",
                "owner2",
                FileStatus.ACTIVE,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                OffsetDateTime.now().plusDays(1)
        );

        FileMetadataResponse res = FileMetadataResponse.fromDto(dto, null);

        assertThat(res.originalName()).isEqualTo("report.pdf");
        assertThat(res.ownerUsername()).isEqualTo("owner2");
    }
}
