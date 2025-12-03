package com.example.dw.application.port;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.dw.application.DwHolidayDirectoryService;
import com.example.dw.application.DwHolidaySnapshot;

import lombok.RequiredArgsConstructor;

/**
 * Adapter implementing DwHolidayPort using DwHolidayDirectoryService.
 */
@Component
@RequiredArgsConstructor
public class DwHolidayPortAdapter implements DwHolidayPort {

    private final DwHolidayDirectoryService holidayService;

    @Override
    public Optional<DwHolidayRecord> findByDateAndCountry(LocalDate holidayDate, String countryCode) {
        return holidayService.findByDateAndCountry(holidayDate, countryCode).map(this::toRecord);
    }

    @Override
    public boolean isHoliday(LocalDate date, String countryCode) {
        return holidayService.isHoliday(date, countryCode);
    }

    @Override
    public List<DwHolidayRecord> findByCountry(String countryCode) {
        return holidayService.findByCountry(countryCode).stream().map(this::toRecord).toList();
    }

    @Override
    public List<DwHolidayRecord> findByCountryAndDateRange(String countryCode, LocalDate startDate, LocalDate endDate) {
        return holidayService.findByCountryAndDateRange(countryCode, startDate, endDate)
                .stream().map(this::toRecord).toList();
    }

    @Override
    public List<DwHolidayRecord> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return holidayService.findByDateRange(startDate, endDate).stream().map(this::toRecord).toList();
    }

    @Override
    public List<String> findAvailableCountryCodes() {
        return holidayService.findAvailableCountryCodes();
    }

    private DwHolidayRecord toRecord(DwHolidaySnapshot snapshot) {
        return new DwHolidayRecord(snapshot.id(), snapshot.holidayDate(), snapshot.countryCode(),
                snapshot.localName(), snapshot.englishName(), snapshot.workingDay(), snapshot.syncedAt());
    }
}
