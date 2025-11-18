package com.example.batch.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.hr.dto.HrEmployeeRecord;

class HrRecordValidatorTest {

    private final HrRecordValidator validator = new HrRecordValidator();

    @Test
    void givenValidRecords_whenValidate_thenReturnValidList() {
        HrEmployeeRecord record = new HrEmployeeRecord("E-1", "Kim", "kim@example.com", "ORG",
                "FULL", "ACTIVE", LocalDate.now(), null, "payload", 2);

        var result = validator.validate(List.of(record));

        assertThat(result.validRecords()).containsExactly(record);
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void givenInvalidRecords_whenValidate_thenCollectErrors() {
        HrEmployeeRecord invalid = new HrEmployeeRecord(null, "", null, "ORG",
                "FULL", "ACTIVE", null, LocalDate.now().minusDays(1), "bad", 3);
        HrEmployeeRecord duplicate = new HrEmployeeRecord("E-1", "Kim", "kim@example.com", "ORG",
                "FULL", "ACTIVE", LocalDate.now(), null, "payload", 4);

        var result = validator.validate(List.of(invalid, duplicate, duplicate));

        assertThat(result.validRecords()).hasSize(1);
        assertThat(result.errors()).hasSize(2);
        assertThat(result.errors().getFirst().errorMessage()).contains("Employee ID is required");
        assertThat(result.errors().get(1).errorMessage()).contains("Duplicate employee record");
    }
}
