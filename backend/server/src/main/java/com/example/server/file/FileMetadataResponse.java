package com.example.server.file;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.common.file.FileMetadataDto;
import com.example.common.file.FileStatus;

public record FileMetadataResponse(UUID id,
                                   String originalName,
                                   String contentType,
                                   long size,
                                   String checksum,
                                   String ownerUsername,
                                   FileStatus status,
                                   OffsetDateTime retentionUntil,
                                   OffsetDateTime createdAt,
                                   OffsetDateTime updatedAt) {

    public static FileMetadataResponse fromDto(FileMetadataDto dto) {
        return new FileMetadataResponse(
                dto.id(),
                dto.originalName(),
                dto.contentType(),
                dto.size(),
                dto.checksum(),
                dto.ownerUsername(),
                dto.status(),
                dto.retentionUntil(),
                dto.createdAt(),
                dto.updatedAt());
    }
}
