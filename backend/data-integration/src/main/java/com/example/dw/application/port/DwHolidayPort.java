package com.example.dw.application.port;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port interface for holiday queries.
 */
public interface DwHolidayPort {

    Optional<DwHolidayRecord> findByDateAndCountry(LocalDate holidayDate, String countryCode);

    boolean isHoliday(LocalDate date, String countryCode);

    List<DwHolidayRecord> findByCountry(String countryCode);

    List<DwHolidayRecord> findByCountryAndDateRange(String countryCode, LocalDate startDate, LocalDate endDate);

    List<DwHolidayRecord> findByDateRange(LocalDate startDate, LocalDate endDate);

    List<String> findAvailableCountryCodes();

    record DwHolidayRecord(UUID id,
                           LocalDate holidayDate,
                           String countryCode,
                           String localName,
                           String englishName,
                           boolean workingDay,
                           OffsetDateTime syncedAt) { }
}
