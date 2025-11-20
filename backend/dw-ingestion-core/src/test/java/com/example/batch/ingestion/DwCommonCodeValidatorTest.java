package com.example.batch.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.dw.dto.DwCommonCodeRecord;

class DwCommonCodeValidatorTest {

    private final DwCommonCodeValidator validator = new DwCommonCodeValidator();

    @Test
    void givenValidRecords_whenValidate_thenReturnAllValid() {
        DwCommonCodeRecord record = new DwCommonCodeRecord("CATEGORY", "A", "Alpha", 1, true, null, null, null, 2);

        DwCommonCodeValidationResult result = validator.validate(List.of(record));

        assertThat(result.validRecords()).hasSize(1);
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void givenDuplicateKey_whenValidate_thenReturnError() {
        DwCommonCodeRecord record1 = new DwCommonCodeRecord("CATEGORY", "A", "Alpha", 1, true, null, null, null, 2);
        DwCommonCodeRecord record2 = new DwCommonCodeRecord("category", "a", "Beta", 2, true, null, null, null, 3);

        DwCommonCodeValidationResult result = validator.validate(List.of(record1, record2));

        assertThat(result.validRecords()).hasSize(1);
        assertThat(result.errors()).hasSize(1);
    }
}
