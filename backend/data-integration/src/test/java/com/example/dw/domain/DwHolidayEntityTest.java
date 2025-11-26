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
        DwHolidayEntity entity = DwHolidayEntity.create(
                LocalDate.parse("2024-02-01"),
                "KR",
                "설날",
                "Lunar New Year",
                false,
                UUID.randomUUID(),
                OffsetDateTime.of(2024, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        );

        assertThat(entity.sameBusinessState("설날", "Lunar New Year", false)).isTrue();
        assertThat(entity.sameBusinessState("추석", "Chuseok", false)).isFalse();
    }

    @Test
    @DisplayName("updateFromRecord는 필드를 갱신하고 syncedAt을 변경한다")
    void updateFromRecordUpdatesFields() {
        UUID batchId = UUID.randomUUID();
        OffsetDateTime synced = OffsetDateTime.of(2024, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        DwHolidayEntity entity = DwHolidayEntity.create(
                LocalDate.parse("2024-05-05"),
                "KR",
                "어린이날",
                "Children's Day",
                false,
                batchId,
                synced
        );

        entity.updateFromRecord("휴일", "Holiday", true, batchId, synced.plusDays(1));

        assertThat(entity.getLocalName()).isEqualTo("휴일");
        assertThat(entity.getEnglishName()).isEqualTo("Holiday");
        assertThat(entity.isWorkingDay()).isTrue();
        assertThat(entity.getSyncedAt()).isEqualTo(synced.plusDays(1));
    }
}
