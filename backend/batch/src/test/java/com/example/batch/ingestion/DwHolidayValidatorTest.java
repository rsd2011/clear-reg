package com.example.batch.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.dw.dto.DwHolidayRecord;

class DwHolidayValidatorTest {

    private final DwHolidayValidator validator = new DwHolidayValidator();

    @DisplayName("유효한 레코드는 통과하고 중복/누락은 오류로 분류한다")
    @Test
    void validate_mixedRecords_collectsErrors() {
        DwHolidayRecord valid = new DwHolidayRecord(LocalDate.of(2024, 1, 1), "KR", "신정", "New Year", false);
        DwHolidayRecord missingCountry = new DwHolidayRecord(LocalDate.of(2024, 2, 9), " ", "설", null, true);
        DwHolidayRecord duplicate = new DwHolidayRecord(LocalDate.of(2024, 1, 1), "kr", "신정", null, false);
        DwHolidayRecord missingName = new DwHolidayRecord(LocalDate.of(2024, 3, 1), "KR", "", null, false);

        DwHolidayValidationResult result = validator.validate(List.of(valid, missingCountry, duplicate, missingName));

        assertThat(result.validRecords()).containsExactly(valid);
        assertThat(result.errors()).hasSize(3);
        assertThat(result.errors().get(0).message()).contains("국가 코드");
        assertThat(result.errors().get(1).message()).contains("중복");
        assertThat(result.errors().get(2).message()).contains("휴일명");
    }
}
