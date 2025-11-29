package com.example.common.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MaskingQuery")
class MaskingQueryTest {

    @Test
    @DisplayName("모든 필드를 설정하여 생성할 수 있다")
    void createsWithAllFields() {
        Instant now = Instant.now();
        MaskingQuery query = new MaskingQuery(
                "ORGANIZATION",
                "READ",
                "ADMIN",
                List.of("ORG_A", "ORG_B"),
                "SSN",
                now
        );

        assertThat(query.featureCode()).isEqualTo("ORGANIZATION");
        assertThat(query.actionCode()).isEqualTo("READ");
        assertThat(query.permGroupCode()).isEqualTo("ADMIN");
        assertThat(query.orgGroupCodes()).containsExactly("ORG_A", "ORG_B");
        assertThat(query.dataKind()).isEqualTo("SSN");
        assertThat(query.now()).isEqualTo(now);
    }

    @Test
    @DisplayName("nowOrDefault는 now가 null이면 현재 시간을 반환한다")
    void nowOrDefaultReturnsCurrentTimeWhenNull() {
        MaskingQuery query = new MaskingQuery(
                "ORGANIZATION",
                null,
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
        MaskingQuery query = new MaskingQuery(
                "ORGANIZATION",
                null,
                null,
                null,
                "PHONE",
                specificTime
        );

        assertThat(query.nowOrDefault()).isEqualTo(specificTime);
    }

    @Test
    @DisplayName("equals와 hashCode가 올바르게 동작한다")
    void equalsAndHashCode() {
        Instant now = Instant.now();
        MaskingQuery query1 = new MaskingQuery("ORGANIZATION", "READ", "PG", List.of("ORG"), "SSN", now);
        MaskingQuery query2 = new MaskingQuery("ORGANIZATION", "READ", "PG", List.of("ORG"), "SSN", now);

        assertThat(query1).isEqualTo(query2);
        assertThat(query1.hashCode()).isEqualTo(query2.hashCode());
    }

    @Test
    @DisplayName("dataKind가 null일 수 있다")
    void dataKindCanBeNull() {
        MaskingQuery query = new MaskingQuery(
                "ORGANIZATION",
                null,
                null,
                null,
                null,
                Instant.now()
        );

        assertThat(query.dataKind()).isNull();
    }
}
