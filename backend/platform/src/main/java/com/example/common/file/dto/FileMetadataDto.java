package com.example.common.file.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.common.file.FileStatus;

public record FileMetadataDto(UUID id,
                              String originalName,
                              String contentType,
                              long size,
                              String checksum,
                              String ownerUsername,
                              FileStatus status,
                              OffsetDateTime retentionUntil,
                              OffsetDateTime createdAt,
                              OffsetDateTime updatedAt) {
}
