package com.example.dw.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DwHolidayEntityTest {

    @Test
    void givenMatchingFields_whenComparingBusinessState_thenTrue() {
        DwHolidayEntity entity = new DwHolidayEntity();
        entity.setLocalName("Holiday");
        entity.setEnglishName("Holiday");
        entity.setWorkingDay(false);

        assertThat(entity.sameBusinessState("Holiday", "Holiday", false)).isTrue();
    }

    @Test
    void givenDifferentFields_whenComparingBusinessState_thenFalse() {
        DwHolidayEntity entity = new DwHolidayEntity();
        entity.setLocalName("Holiday");
        entity.setEnglishName("Holiday");
        entity.setWorkingDay(false);

        assertThat(entity.sameBusinessState("Holiday", "Vacation", false)).isFalse();
    }
}
