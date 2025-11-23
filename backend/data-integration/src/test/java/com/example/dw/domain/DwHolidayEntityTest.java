package com.example.dw.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DwHolidayEntityTest {

    @Test
    @DisplayName("휴일 엔티티는 비즈니스 상태 비교를 제공한다")
    void sameBusinessStateMatchesFields() {
        DwHolidayEntity entity = new DwHolidayEntity();
        entity.setHolidayDate(LocalDate.parse("2024-02-01"));
        entity.setCountryCode("KR");
        entity.setLocalName("설날");
        entity.setEnglishName("Lunar New Year");
        entity.setWorkingDay(false);
        entity.setSyncedAt(OffsetDateTime.of(2024, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        entity.setSourceBatchId(UUID.randomUUID());

        assertThat(entity.sameBusinessState("설날", "Lunar New Year", false)).isTrue();
        assertThat(entity.sameBusinessState("추석", "Chuseok", false)).isFalse();
    }
}
