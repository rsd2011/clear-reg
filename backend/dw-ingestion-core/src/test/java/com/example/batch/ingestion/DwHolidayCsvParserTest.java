package com.example.batch.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.dw.dto.DwHolidayRecord;

class DwHolidayCsvParserTest {

    private final DwHolidayCsvParser parser = new DwHolidayCsvParser();

    @DisplayName("헤더를 건너뛰고 휴일 레코드를 파싱한다")
    @Test
    void parse_validLines_returnsRecords() {
        String payload = "date,country,local,english,working\n" +
                "20240101,kr,신정,New Year,false\n" +
                "20240210,kr,설날,,true";

        List<DwHolidayRecord> records = parser.parse(payload);

        assertThat(records).hasSize(2);
        assertThat(records.get(0).countryCode()).isEqualTo("KR");
        assertThat(records.get(0).workingDay()).isFalse();
        assertThat(records.get(1).englishName()).isEmpty();
        assertThat(records.get(1).workingDay()).isTrue();
    }

    @DisplayName("빈 페이로드는 빈 리스트를 반환한다")
    @Test
    void parse_blankPayload_returnsEmpty() {
        assertThat(parser.parse("  \n  ")).isEmpty();
    }

    @DisplayName("잘못된 날짜 형식이면 예외를 던진다")
    @Test
    void parse_invalidDate_throws() {
        String payload = "date,country,local\ninvalid,kr,무효";

        assertThatThrownBy(() -> parser.parse(payload))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("휴일 CSV를 파싱하지 못했습니다.");
    }

    @DisplayName("컬럼이 3개 미만이면 레코드를 건너뛴다")
    @Test
    void parse_tooShortRow_skips() {
        String payload = "date,country\n20240101,KR";

        assertThat(parser.parse(payload)).isEmpty();
    }
}
