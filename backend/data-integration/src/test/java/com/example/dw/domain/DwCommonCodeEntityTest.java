package com.example.dw.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DwCommonCodeEntityTest {

    @Test
    void givenSameBusinessState_whenCompared_thenTrue() {
        DwCommonCodeEntity entity = new DwCommonCodeEntity();
        entity.setCodeName("정상");
        entity.setDisplayOrder(1);
        entity.setActive(true);
        entity.setCategory("DEFAULT");
        entity.setDescription("desc");
        entity.setMetadataJson("{\"k\":\"v\"}");

        assertThat(entity.sameBusinessState("정상", 1, true, "DEFAULT", "desc", "{\"k\":\"v\"}")).isTrue();
    }

    @Test
    void givenDifferentState_whenCompared_thenFalse() {
        DwCommonCodeEntity entity = new DwCommonCodeEntity();
        entity.setCodeName("정상");
        entity.setDisplayOrder(1);
        entity.setActive(true);

        assertThat(entity.sameBusinessState("정상", 2, true, null, null, null)).isFalse();
    }
}
