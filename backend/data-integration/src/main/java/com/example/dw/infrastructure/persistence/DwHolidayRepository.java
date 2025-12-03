package com.example.dw.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.dw.domain.DwHolidayEntity;

public interface DwHolidayRepository extends JpaRepository<DwHolidayEntity, UUID> {

    Optional<DwHolidayEntity> findFirstByHolidayDateAndCountryCode(LocalDate holidayDate, String countryCode);

    /**
     * Find all holidays for a specific country code.
     */
    List<DwHolidayEntity> findByCountryCodeOrderByHolidayDateAsc(String countryCode);

    /**
     * Find holidays within a date range for a specific country.
     */
    @Query("SELECT h FROM DwHolidayEntity h WHERE h.countryCode = :countryCode " +
           "AND h.holidayDate BETWEEN :startDate AND :endDate ORDER BY h.holidayDate ASC")
    List<DwHolidayEntity> findByCountryCodeAndDateRange(
            @Param("countryCode") String countryCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find holidays within a date range for all countries.
     */
    @Query("SELECT h FROM DwHolidayEntity h WHERE h.holidayDate BETWEEN :startDate AND :endDate " +
           "ORDER BY h.holidayDate ASC, h.countryCode ASC")
    List<DwHolidayEntity> findByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Check if a specific date is a holiday for a given country.
     */
    boolean existsByHolidayDateAndCountryCode(LocalDate holidayDate, String countryCode);

    /**
     * Paginated query for holidays by country.
     */
    Page<DwHolidayEntity> findByCountryCode(String countryCode, Pageable pageable);

    /**
     * Find all distinct country codes.
     */
    @Query("SELECT DISTINCT h.countryCode FROM DwHolidayEntity h ORDER BY h.countryCode")
    List<String> findDistinctCountryCodes();
}
