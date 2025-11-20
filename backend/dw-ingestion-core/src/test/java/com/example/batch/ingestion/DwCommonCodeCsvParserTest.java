package com.example.batch.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.dw.dto.DwCommonCodeRecord;

class DwCommonCodeCsvParserTest {

    private final DwCommonCodeCsvParser parser = new DwCommonCodeCsvParser();

    @Test
    void givenValidCsv_whenParse_thenReturnRecords() {
        String payload = """
                codeType,codeValue,codeName,order,active,category,description,metadata
                CATEGORY,A,Alpha,1,true,DEFAULT,설명,{"a":1}
                CATEGORY,B,Beta,2,false,,,
                """;

        List<DwCommonCodeRecord> records = parser.parse(payload);

        assertThat(records).hasSize(2);
        assertThat(records.getFirst().codeValue()).isEqualTo("A");
        assertThat(records.get(1).active()).isFalse();
    }
}
