package com.example.dw.infrastructure.persistence;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.dw.domain.DwHolidayEntity;

public interface DwHolidayRepository extends JpaRepository<DwHolidayEntity, UUID> {

    Optional<DwHolidayEntity> findFirstByHolidayDateAndCountryCode(LocalDate holidayDate, String countryCode);
}
