package com.example.admin.systemconfig.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.admin.systemconfig.domain.SystemConfigRoot;

/**
 * 시스템 설정 루트 응답 DTO.
 */
public record SystemConfigRootResponse(
    UUID id,
    String configCode,
    String name,
    String description,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    Integer currentVersionNumber,
    boolean hasDraft,
    boolean active
) {
  public static SystemConfigRootResponse from(SystemConfigRoot root) {
    return new SystemConfigRootResponse(
        root.getId(),
        root.getConfigCode(),
        root.getName(),
        root.getDescription(),
        root.getCreatedAt(),
        root.getUpdatedAt(),
        root.getCurrentVersionNumber(),
        root.hasDraft(),
        root.isActive()
    );
  }
}
