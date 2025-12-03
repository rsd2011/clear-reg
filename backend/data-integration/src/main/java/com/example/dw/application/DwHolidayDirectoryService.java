package com.example.dw.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.example.audit.Actor;
import com.example.audit.ActorType;
import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPort;
import com.example.audit.RiskLevel;
import com.example.audit.Subject;
import com.example.common.cache.CacheNames;
import com.example.dw.infrastructure.persistence.DwHolidayRepository;

/**
 * Service for querying holiday information from the DW system.
 * Provides caching and audit logging for holiday lookups.
 */
@Service
@RequiredArgsConstructor
public class DwHolidayDirectoryService {

    private final DwHolidayRepository holidayRepository;
    private final AuditPort auditPort;

    /**
     * Find a specific holiday by date and country code.
     *
     * @param holidayDate the date to check
     * @param countryCode the ISO country code (e.g., "KR", "US")
     * @return the holiday snapshot if found
     */
    @Cacheable(cacheNames = CacheNames.DW_HOLIDAYS, 
               key = "#holidayDate.toString() + '_' + #countryCode", 
               unless = "#result.isEmpty()")
    public Optional<DwHolidaySnapshot> findByDateAndCountry(LocalDate holidayDate, String countryCode) {
        Optional<DwHolidaySnapshot> result = holidayRepository
                .findFirstByHolidayDateAndCountryCode(holidayDate, countryCode)
                .map(DwHolidaySnapshot::fromEntity);
        auditLookup("FIND_BY_DATE_COUNTRY", holidayDate + "_" + countryCode, result.isPresent());
        return result;
    }

    /**
     * Check if a specific date is a holiday for a given country.
     *
     * @param date the date to check
     * @param countryCode the ISO country code
     * @return true if the date is a holiday
     */
    public boolean isHoliday(LocalDate date, String countryCode) {
        boolean result = holidayRepository.existsByHolidayDateAndCountryCode(date, countryCode);
        auditLookup("IS_HOLIDAY", date + "_" + countryCode, true);
        return result;
    }

    /**
     * Find all holidays for a specific country.
     *
     * @param countryCode the ISO country code
     * @return list of holiday snapshots ordered by date
     */
    public List<DwHolidaySnapshot> findByCountry(String countryCode) {
        List<DwHolidaySnapshot> result = holidayRepository
                .findByCountryCodeOrderByHolidayDateAsc(countryCode)
                .stream()
                .map(DwHolidaySnapshot::fromEntity)
                .toList();
        auditLookup("FIND_BY_COUNTRY", countryCode, !result.isEmpty());
        return result;
    }

    /**
     * Find holidays within a date range for a specific country.
     *
     * @param countryCode the ISO country code
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return list of holiday snapshots within the range
     */
    public List<DwHolidaySnapshot> findByCountryAndDateRange(String countryCode, 
                                                              LocalDate startDate, 
                                                              LocalDate endDate) {
        List<DwHolidaySnapshot> result = holidayRepository
                .findByCountryCodeAndDateRange(countryCode, startDate, endDate)
                .stream()
                .map(DwHolidaySnapshot::fromEntity)
                .toList();
        auditLookup("FIND_BY_COUNTRY_RANGE", countryCode + "_" + startDate + "_" + endDate, !result.isEmpty());
        return result;
    }

    /**
     * Find holidays within a date range for all countries.
     *
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return list of holiday snapshots within the range
     */
    public List<DwHolidaySnapshot> findByDateRange(LocalDate startDate, LocalDate endDate) {
        List<DwHolidaySnapshot> result = holidayRepository
                .findByDateRange(startDate, endDate)
                .stream()
                .map(DwHolidaySnapshot::fromEntity)
                .toList();
        auditLookup("FIND_BY_RANGE", startDate + "_" + endDate, !result.isEmpty());
        return result;
    }

    /**
     * Find holidays for a specific country with pagination.
     *
     * @param countryCode the ISO country code
     * @param pageable pagination parameters
     * @return page of holiday snapshots
     */
    public Page<DwHolidaySnapshot> findByCountry(String countryCode, Pageable pageable) {
        Page<DwHolidaySnapshot> result = holidayRepository
                .findByCountryCode(countryCode, pageable)
                .map(DwHolidaySnapshot::fromEntity);
        auditLookup("FIND_BY_COUNTRY_PAGED", countryCode, !result.isEmpty());
        return result;
    }

    /**
     * Get all distinct country codes that have holiday data.
     *
     * @return list of country codes
     */
    public List<String> findAvailableCountryCodes() {
        return holidayRepository.findDistinctCountryCodes();
    }

    /**
     * Evict cache entry for a specific date and country.
     */
    @CacheEvict(cacheNames = CacheNames.DW_HOLIDAYS, key = "#holidayDate.toString() + '_' + #countryCode")
    public void evict(LocalDate holidayDate, String countryCode) {
        // eviction only
    }

    /**
     * Evict all holiday cache entries.
     */
    @CacheEvict(cacheNames = CacheNames.DW_HOLIDAYS, allEntries = true)
    public void evictAll() {
        // eviction only
    }

    private void auditLookup(String action, String key, boolean success) {
        AuditEvent event = AuditEvent.builder()
                .eventType("DW_HOLIDAY_LOOKUP")
                .moduleName("data-integration")
                .action("DW_HOLIDAY_" + action)
                .actor(Actor.builder().id("dw-holiday-service").type(ActorType.SYSTEM).build())
                .subject(Subject.builder().type("HOLIDAY").key(key).build())
                .success(success)
                .resultCode(success ? "OK" : "NOT_FOUND")
                .riskLevel(RiskLevel.LOW)
                .build();
        try {
            auditPort.record(event, AuditMode.ASYNC_FALLBACK);
        } catch (Exception ignore) {
            // 감사 실패가 업무 흐름을 막지 않도록 삼킴
        }
    }
}
