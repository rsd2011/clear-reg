package com.example.batch.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.dw.dto.DwHolidayRecord;

class DwHolidayValidatorTest {

    private final DwHolidayValidator validator = new DwHolidayValidator();

    @Test
    void givenValidRecords_whenValidate_thenReturnValidList() {
        DwHolidayRecord record = new DwHolidayRecord(LocalDate.of(2024, 1, 1), "US", "New Year", "New Year", false);

        DwHolidayValidationResult result = validator.validate(List.of(record));

        assertThat(result.validRecords()).hasSize(1);
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void givenDuplicateRecords_whenValidate_thenReturnErrors() {
        DwHolidayRecord record1 = new DwHolidayRecord(LocalDate.of(2024, 1, 1), "US", "New Year", "New Year", false);
        DwHolidayRecord record2 = new DwHolidayRecord(LocalDate.of(2024, 1, 1), "US", "New Year", "New Year", false);

        DwHolidayValidationResult result = validator.validate(List.of(record1, record2));

        assertThat(result.validRecords()).hasSize(1);
        assertThat(result.errors()).hasSize(1);
    }
}
