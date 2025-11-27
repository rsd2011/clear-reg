package com.example.server.file.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.UnaryOperator;

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
        return fromDto(dto, UnaryOperator.identity());
    }

    public static FileMetadataResponse fromDto(FileMetadataDto dto, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        return new FileMetadataResponse(
                dto.id(),
                fn.apply(dto.originalName()),
                dto.contentType(),
                dto.size(),
                dto.checksum(),
                fn.apply(dto.ownerUsername()),
                dto.status(),
                dto.retentionUntil(),
                dto.createdAt(),
                dto.updatedAt());
    }
}
