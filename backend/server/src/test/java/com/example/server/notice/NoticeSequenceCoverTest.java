package com.example.server.notice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NoticeSequenceCoverTest {

    @Test
    @DisplayName("NoticeSequence 초기값이 있으면 next가 카운터를 증가시킨다")
    void incrementsFromInitial() {
        NoticeSequence seq = new NoticeSequence(0);

        assertThat(seq.next()).isEqualTo(1L);
        assertThat(seq.next()).isEqualTo(2L);
    }
}
