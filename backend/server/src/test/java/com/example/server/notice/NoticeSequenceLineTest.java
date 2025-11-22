package com.example.server.notice;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NoticeSequenceLineTest {

    @Test
    @DisplayName("해당 날짜로 생성된 시퀀스는 next 호출 시 카운터가 증가한다")
    void nextIncrementsCounter() {
        NoticeSequence seq = new NoticeSequence(0);

        assertThat(seq.next()).isEqualTo(1L);
        assertThat(seq.next()).isEqualTo(2L);
    }
}
