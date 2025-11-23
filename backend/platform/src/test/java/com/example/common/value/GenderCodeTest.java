package com.example.common.value;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GenderCode 브랜치")
class GenderCodeTest {

    @Test
    @DisplayName("null/빈/잘못된 값은 UNKNOWN")
    void genderBranches() {
        assertThat(GenderCode.from(null)).isEqualTo(GenderCode.UNKNOWN);
        assertThat(GenderCode.from(" ")).isEqualTo(GenderCode.UNKNOWN);
        assertThat(GenderCode.from("invalid")).isEqualTo(GenderCode.UNKNOWN);
        assertThat(GenderCode.from("male")).isEqualTo(GenderCode.MALE);
        assertThat(GenderCode.MALE.jsonValue()).isEqualTo("MALE");
    }
}

