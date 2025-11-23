package com.example.dw.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HrExternalFeedStatusTest {

    @Test
    @DisplayName("HrExternalFeedStatus enum valueOf 매핑이 동작한다")
    void enumValueOf() {
        assertThat(HrExternalFeedStatus.valueOf("PENDING")).isEqualTo(HrExternalFeedStatus.PENDING);
        assertThat(HrExternalFeedStatus.values()).contains(HrExternalFeedStatus.COMPLETED);
    }
}
