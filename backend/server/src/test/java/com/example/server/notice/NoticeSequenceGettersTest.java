package com.example.server.notice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NoticeSequenceGettersTest {

    @Test
    @DisplayName("시퀀스 생성 후 getter들이 현재 상태를 반환한다")
    void gettersReturnState() {
        NoticeSequence seq = new NoticeSequence(2025);

        // When : next를 한 번 호출해 값을 증가시키고
        int first = seq.next();

        // Then : sequenceYear와 nextValue, version 기본값을 확인한다
        assertThat(first).isEqualTo(1);
        assertThat(seq.getSequenceYear()).isEqualTo(2025);
        assertThat(seq.getNextValue()).isEqualTo(2);
        assertThat(seq.getVersion()).isZero();
    }
}
