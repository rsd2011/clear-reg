package com.example.dw.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DwCommonCodeEntityTest {

    @Test
    void givenSameBusinessState_whenCompared_thenTrue() {
        DwCommonCodeEntity entity = DwCommonCodeEntity.create(
                "TYPE",
                "VAL",
                "정상",
                1,
                true,
                "DEFAULT",
                "desc",
                "{\"k\":\"v\"}",
                java.util.UUID.randomUUID(),
                java.time.OffsetDateTime.now()
        );

        assertThat(entity.sameBusinessState("정상", 1, true, "DEFAULT", "desc", "{\"k\":\"v\"}")).isTrue();
    }

    @Test
    void givenDifferentState_whenCompared_thenFalse() {
        DwCommonCodeEntity entity = DwCommonCodeEntity.create(
                "TYPE",
                "VAL",
                "정상",
                1,
                true,
                null,
                null,
                null,
                java.util.UUID.randomUUID(),
                java.time.OffsetDateTime.now()
        );

        assertThat(entity.sameBusinessState("정상", 2, true, null, null, null)).isFalse();
    }
}
