package com.example.batch.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class HrCsvRecordParserTest {

    private final HrCsvRecordParser parser = new HrCsvRecordParser();

    @Test
    void givenCsvPayload_whenParse_thenReturnRecords() {
        String payload = "employeeId,fullName,email,org,type,status,start,end\n" +
                "E-1,Kim,kim@example.com,ORG,FULL,ACTIVE,2023-01-01,\n";

        List<?> records = parser.parse(payload);

        assertThat(records).hasSize(1);
    }

    @Test
    void givenInvalidPayload_whenParse_thenSkipMalformedRows() {
        String payload = "header\ninvalid";
        assertThat(parser.parse(payload)).isEmpty();
    }

    @Test
    void givenNullPayload_whenParse_thenReturnEmptyList() {
        assertThat(parser.parse(null)).isEmpty();
    }

    @Test
    void givenUnexpectedException_whenParse_thenWrapAsIllegalState() {
        String payload = "employeeId,fullName,email,org,type,status,start,end\n" +
                "E-1,Kim,kim@example.com,ORG,FULL,ACTIVE,invalid-date,\n";
        assertThatThrownBy(() -> parser.parse(payload))
                .isInstanceOf(IllegalStateException.class);
    }
}
