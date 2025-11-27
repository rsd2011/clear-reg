package com.example.batch.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.dw.dto.HrOrganizationRecord;

class HrOrganizationValidatorTest {

    private final HrOrganizationValidator validator = new HrOrganizationValidator();

    @Test
    void givenValidRecords_whenValidate_thenReturnValidList() {
        HrOrganizationRecord record = new HrOrganizationRecord("ORG001", "HQ", null, "ACTIVE",
                null, null, LocalDate.of(2024, 1, 1), null, "payload", 2);

        var result = validator.validate(List.of(record));

        assertThat(result.validRecords()).hasSize(1);
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void givenInvalidRecord_whenValidate_thenReturnErrors() {
        HrOrganizationRecord record = new HrOrganizationRecord(null, null, null, "ACTIVE",
                null, null, null, null, "payload", 2);

        var result = validator.validate(List.of(record));

        assertThat(result.validRecords()).isEmpty();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).errorMessage()).contains("Organization code is required");
    }

    @Test
    void givenDuplicateAndInvalidDates_whenValidate_thenFlagsErrors() {
        HrOrganizationRecord base = new HrOrganizationRecord("ORG001", "HQ", null, "ACTIVE",
                null, null, LocalDate.of(2024, 1, 1), LocalDate.of(2023, 12, 31), "payload1", 2);
        HrOrganizationRecord duplicate = new HrOrganizationRecord("ORG001", "HQ2", null, "ACTIVE",
                null, null, LocalDate.of(2024, 1, 1), null, "payload2", 3);

        var result = validator.validate(List.of(base, duplicate));

        assertThat(result.validRecords()).hasSize(0);
        assertThat(result.errors()).hasSize(2);
        assertThat(result.errors().get(0).errorMessage()).contains("End date must not be before start date");
        assertThat(result.errors().get(1).errorMessage()).contains("Duplicate organization snapshot");
    }
}
