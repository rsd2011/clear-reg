package com.example.batch.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.dw.dto.DwCommonCodeRecord;

class DwCommonCodeValidatorTest {

    private final DwCommonCodeValidator validator = new DwCommonCodeValidator();

    @DisplayName("중복이 없고 필수 필드가 있는 항목만 유효 목록에 포함된다")
    @Test
    void validate_filtersInvalidAndDuplicates() {
        DwCommonCodeRecord valid = new DwCommonCodeRecord("COUNTRY", "KR", "대한민국", 1, true, null, null, null, 1);
        DwCommonCodeRecord noType = new DwCommonCodeRecord(" ", "KR", "누락", 0, true, null, null, null, 2);
        DwCommonCodeRecord duplicate = new DwCommonCodeRecord("COUNTRY", "KR", "중복", 0, true, null, null, null, 3);
        DwCommonCodeRecord noName = new DwCommonCodeRecord("COUNTRY", "US", "", 0, true, null, null, null, 4);

        DwCommonCodeValidationResult result = validator.validate(List.of(valid, noType, duplicate, noName));

        assertThat(result.validRecords()).containsExactly(valid);
        assertThat(result.errors()).hasSize(3);
        assertThat(result.errors().get(0).message()).contains("코드 유형");
        assertThat(result.errors().get(1).message()).contains("중복");
        assertThat(result.errors().get(2).message()).contains("코드명");
    }
}
