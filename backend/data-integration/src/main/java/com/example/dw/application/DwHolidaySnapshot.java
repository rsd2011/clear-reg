package com.example.dw.application;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.dw.domain.DwHolidayEntity;

/**
 * Lightweight immutable projection of a holiday row suitable for caching.
 */
public record DwHolidaySnapshot(UUID id,
                                LocalDate holidayDate,
                                String countryCode,
                                String localName,
                                String englishName,
                                boolean workingDay,
                                OffsetDateTime syncedAt) {

    public static DwHolidaySnapshot fromEntity(DwHolidayEntity entity) {
        return new DwHolidaySnapshot(
                entity.getId(),
                entity.getHolidayDate(),
                entity.getCountryCode(),
                entity.getLocalName(),
                entity.getEnglishName(),
                entity.isWorkingDay(),
                entity.getSyncedAt());
    }
}
