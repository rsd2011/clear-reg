package com.example.batch.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.dw.dto.DwCommonCodeRecord;

class DwCommonCodeCsvParserTest {

    private final DwCommonCodeCsvParser parser = new DwCommonCodeCsvParser();

    @DisplayName("헤더 이후 레코드를 파싱하며 boolean/int 값을 해석한다")
    @Test
    void parse_validPayload_returnsRecords() {
        String payload = "type,value,name,order,active,category,desc,meta\n" +
                "COUNTRY,KR,대한민국,1,false,ASIA,desc,{\"k\":1}\n" +
                "CURRENCY,USD,달러,, , , ";

        List<DwCommonCodeRecord> records = parser.parse(payload);

        assertThat(records).hasSize(2);
        DwCommonCodeRecord first = records.get(0);
        assertThat(first.displayOrder()).isEqualTo(1);
        assertThat(first.active()).isFalse();
        DwCommonCodeRecord second = records.get(1);
        assertThat(second.displayOrder()).isZero();
        assertThat(second.active()).isTrue();
    }

    @DisplayName("빈 페이로드는 빈 리스트를 반환한다")
    @Test
    void parse_blankPayload_returnsEmpty() {
        assertThat(parser.parse("   ")).isEmpty();
    }

    @DisplayName("잘못된 숫자 형식이면 IllegalStateException을 던진다")
    @Test
    void parse_invalidInteger_throws() {
        String payload = "type,value,name,order\nCOUNTRY,KR,대한민국,notNumber";

        assertThatThrownBy(() -> parser.parse(payload))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to parse DW common-code payload");
    }

    @DisplayName("컬럼이 부족한 레코드는 건너뛴다")
    @Test
    void parse_missingColumns_skipsLine() {
        String payload = "type,value,name\nCOUNTRY,KR";

        assertThat(parser.parse(payload)).isEmpty();
    }
}
