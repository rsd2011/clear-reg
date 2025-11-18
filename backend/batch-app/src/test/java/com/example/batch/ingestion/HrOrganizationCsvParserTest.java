package com.example.batch.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.hr.dto.HrOrganizationRecord;

class HrOrganizationCsvParserTest {

    private final HrOrganizationCsvParser parser = new HrOrganizationCsvParser();

    @Test
    void givenCsv_whenParse_thenReturnRecords() {
        String payload = "code,name,parent,status,start,end\n" +
                "ORG001,Headquarters,,ACTIVE,2024-01-01,\n";

        List<HrOrganizationRecord> records = parser.parse(payload);

        assertThat(records).hasSize(1);
    }
}
