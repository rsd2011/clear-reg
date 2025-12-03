package com.example.batch.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.dw.dto.HrOrganizationRecord;

class HrOrganizationCsvParserTest {

    private final HrOrganizationCsvParser parser = new HrOrganizationCsvParser();

    @Test
    void givenCsv_whenParse_thenReturnRecords() {
        String payload = "code,name,parent,status,leader,manager,start,end\n" +
                "ORG001,Headquarters,,ACTIVE,EMP001,,2024-01-01,\n";

        List<HrOrganizationRecord> records = parser.parse(payload);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).leaderEmployeeId()).isEqualTo("EMP001");
        assertThat(records.get(0).managerEmployeeId()).isEmpty();
    }

    @Test
    void givenEmptyPayload_whenParse_thenReturnEmpty() {
        assertThat(parser.parse("   ")).isEmpty();
    }

    @Test
    void givenTooFewColumns_whenParse_thenSkipLine() {
        String payload = "code,name,parent,status,leader,manager,start,end\n" +
                "ORG002,Branch"; // columns fewer than expected (8 required)

        List<HrOrganizationRecord> records = parser.parse(payload);

        assertThat(records).isEmpty();
    }
}
