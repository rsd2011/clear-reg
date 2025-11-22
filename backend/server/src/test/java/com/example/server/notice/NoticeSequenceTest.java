package com.example.server.notice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NoticeSequenceTest {

    @Test
    @DisplayName("next를 호출할 때마다 값이 증가한다")
    void nextIncrements() {
        NoticeSequence sequence = new NoticeSequence(2025);

        int first = sequence.next();
        int second = sequence.next();

        assertThat(first).isEqualTo(1);
        assertThat(second).isEqualTo(2);
        assertThat(sequence.getNextValue()).isEqualTo(3);
    }
}
