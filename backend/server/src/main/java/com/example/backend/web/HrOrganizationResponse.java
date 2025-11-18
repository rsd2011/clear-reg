package com.example.backend.web;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.hr.domain.HrOrganizationEntity;

public record HrOrganizationResponse(UUID id,
                                     String organizationCode,
                                     int version,
                                     String name,
                                     String parentOrganizationCode,
                                     String status,
                                     LocalDate effectiveStart,
                                     LocalDate effectiveEnd,
                                     OffsetDateTime syncedAt) {

    public static HrOrganizationResponse fromEntity(HrOrganizationEntity entity) {
        return new HrOrganizationResponse(entity.getId(),
                entity.getOrganizationCode(),
                entity.getVersion(),
                entity.getName(),
                entity.getParentOrganizationCode(),
                entity.getStatus(),
                entity.getEffectiveStart(),
                entity.getEffectiveEnd(),
                entity.getSyncedAt());
    }
}
