package com.example.dw.application;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.example.dw.domain.HrEmployeeEntity;

/**
 * Lightweight immutable projection of an employee row suitable for caching.
 */
public record DwEmployeeSnapshot(String employeeId,
                                 int version,
                                 String fullName,
                                 String email,
                                 String organizationCode,
                                 String employmentType,
                                 String employmentStatus,
                                 LocalDate effectiveStart,
                                 LocalDate effectiveEnd,
                                 OffsetDateTime syncedAt) {

    public static DwEmployeeSnapshot fromEntity(HrEmployeeEntity entity) {
        return new DwEmployeeSnapshot(entity.getEmployeeId(),
                entity.getVersion(),
                entity.getFullName(),
                entity.getEmail(),
                entity.getOrganizationCode(),
                entity.getEmploymentType(),
                entity.getEmploymentStatus(),
                entity.getEffectiveStart(),
                entity.getEffectiveEnd(),
                entity.getSyncedAt());
    }
}
