package com.example.server.notice;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NoticeSequenceRuleTest {

    @Test
    @DisplayName("시퀀스가 비어 있으면 1부터 시작하고, 호출할 때마다 증가한다")
    void generatesAndIncrements() {
        NoticeSequence seq = new NoticeSequence(0);

        long first = seq.next();
        long second = seq.next();

        assertThat(first).isEqualTo(1L);
        assertThat(second).isEqualTo(2L);
    }
}
