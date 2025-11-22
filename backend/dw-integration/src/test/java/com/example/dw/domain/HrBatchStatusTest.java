package com.example.dw.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HrBatchStatusTest {

    @Test
    @DisplayName("HrBatchStatus enum valueOf 매핑이 동작한다")
    void enumValueOf() {
        assertThat(HrBatchStatus.valueOf("RECEIVED")).isEqualTo(HrBatchStatus.RECEIVED);
        assertThat(HrBatchStatus.values()).contains(HrBatchStatus.COMPLETED);
    }
}
