package com.example.batch.ingestion.feed;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.dw.dto.DataFeedType;

class DataFeedTest {

    @DisplayName("attributes 가 없으면 null 을 반환한다")
    @Test
    void attribute_nullWhenAttributesMissing() {
        DataFeed feed = new DataFeed("id", DataFeedType.HOLIDAY, LocalDate.now(), 1, "{}", "SYSTEM", null);

        assertThat(feed.attribute("country")).isNull();
    }

    @DisplayName("존재하는 attribute 키를 조회하면 값을 돌려준다")
    @Test
    void attribute_returnsValue() {
        DataFeed feed = new DataFeed("id", DataFeedType.HOLIDAY, LocalDate.now(), 1, "{}",
                "SYSTEM", Map.of("country", "KR"));

        assertThat(feed.attribute("country")).isEqualTo("KR");
    }
}
