package com.example.dw.application;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.dw.domain.HrOrganizationEntity;

public record DwOrganizationNode(UUID id,
                                 String organizationCode,
                                 int version,
                                 String name,
                                 String parentOrganizationCode,
                                 String status,
                                 LocalDate effectiveStart,
                                 LocalDate effectiveEnd,
                                 UUID sourceBatchId,
                                 OffsetDateTime syncedAt) {

    public static DwOrganizationNode fromEntity(HrOrganizationEntity entity) {
        return new DwOrganizationNode(entity.getId(),
                entity.getOrganizationCode(),
                entity.getVersion(),
                entity.getName(),
                entity.getParentOrganizationCode(),
                entity.getStatus(),
                entity.getEffectiveStart(),
                entity.getEffectiveEnd(),
                entity.getSourceBatchId(),
                entity.getSyncedAt());
    }
}
