package com.example.common.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RowAccessQuery")
class RowAccessQueryTest {

    @Test
    @DisplayName("모든 필드를 설정하여 생성할 수 있다")
    void createsWithAllFields() {
        Instant now = Instant.now();
        RowAccessQuery query = new RowAccessQuery(
                "ORGANIZATION",
                "READ",
                "ADMIN",
                List.of("ORG_A", "ORG_B"),
                now
        );

        assertThat(query.featureCode()).isEqualTo("ORGANIZATION");
        assertThat(query.actionCode()).isEqualTo("READ");
        assertThat(query.permGroupCode()).isEqualTo("ADMIN");
        assertThat(query.orgGroupCodes()).containsExactly("ORG_A", "ORG_B");
        assertThat(query.now()).isEqualTo(now);
    }

    @Test
    @DisplayName("nowOrDefault는 now가 null이면 현재 시간을 반환한다")
    void nowOrDefaultReturnsCurrentTimeWhenNull() {
        RowAccessQuery query = new RowAccessQuery(
                "ORGANIZATION",
                null,
                null,
                null,
                null
        );

        Instant before = Instant.now();
        Instant result = query.nowOrDefault();
        Instant after = Instant.now();

        assertThat(result).isAfterOrEqualTo(before);
        assertThat(result).isBeforeOrEqualTo(after);
    }

    @Test
    @DisplayName("nowOrDefault는 now가 있으면 그 값을 반환한다")
    void nowOrDefaultReturnsNowWhenPresent() {
        Instant specificTime = Instant.parse("2024-01-15T10:30:00Z");
        RowAccessQuery query = new RowAccessQuery(
                "ORGANIZATION",
                null,
                null,
                null,
                specificTime
        );

        assertThat(query.nowOrDefault()).isEqualTo(specificTime);
    }

    @Test
    @DisplayName("equals와 hashCode가 올바르게 동작한다")
    void equalsAndHashCode() {
        Instant now = Instant.now();
        RowAccessQuery query1 = new RowAccessQuery("ORGANIZATION", "READ", "PG", List.of("ORG"), now);
        RowAccessQuery query2 = new RowAccessQuery("ORGANIZATION", "READ", "PG", List.of("ORG"), now);

        assertThat(query1).isEqualTo(query2);
        assertThat(query1.hashCode()).isEqualTo(query2.hashCode());
    }
}
