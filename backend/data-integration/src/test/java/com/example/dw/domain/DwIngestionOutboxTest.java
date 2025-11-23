package com.example.dw.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.dw.application.job.DwIngestionJobType;
import com.example.dw.application.job.DwIngestionOutboxStatus;

class DwIngestionOutboxTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2024-02-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("markRetry는 상태를 PENDING으로 돌리고 availableAt을 지연시킨다")
    void markRetryDelaysAvailability() {
        DwIngestionOutbox outbox = DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, CLOCK);

        outbox.markRetry(CLOCK, Duration.ofMinutes(10), "temporary");

        assertThat(outbox.getStatus()).isEqualTo(DwIngestionOutboxStatus.PENDING);
        assertThat(outbox.getAvailableAt()).isEqualTo(OffsetDateTime.now(CLOCK).plusMinutes(10));
        assertThat(outbox.getRetryCount()).isEqualTo(1);
        assertThat(outbox.getLastError()).isEqualTo("temporary");
    }

    @Test
    @DisplayName("markDeadLetter는 상태를 DEAD_LETTER로, 오류 메시지는 500자로 잘라 저장한다")
    void markDeadLetterTruncatesError() {
        DwIngestionOutbox outbox = DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, CLOCK);
        String longError = "X".repeat(600);

        outbox.markDeadLetter(CLOCK, longError);

        assertThat(outbox.getStatus()).isEqualTo(DwIngestionOutboxStatus.DEAD_LETTER);
        assertThat(outbox.getRetryCount()).isEqualTo(1);
        assertThat(outbox.getLastError()).hasSize(500);
    }

    @Test
    @DisplayName("markFailed는 상태를 FAILED로 변경하고 에러 메시지를 저장한다")
    void markFailedStoresError() {
        DwIngestionOutbox outbox = DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, CLOCK);

        outbox.markFailed(CLOCK, "failure");

        assertThat(outbox.getStatus()).isEqualTo(DwIngestionOutboxStatus.FAILED);
        assertThat(outbox.getLastError()).isEqualTo("failure");
    }

    @Test
    @DisplayName("markSending은 SENDING 상태와 잠금 정보를 설정한다")
    void markSendingSetsLockInfo() {
        DwIngestionOutbox outbox = DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, CLOCK);

        outbox.markSending(CLOCK, "locker");

        assertThat(outbox.getStatus()).isEqualTo(DwIngestionOutboxStatus.SENDING);
        assertThat(outbox.getLockedBy()).isEqualTo("locker");
        assertThat(outbox.getLockedAt()).isEqualTo(OffsetDateTime.now(CLOCK));
    }

    @Test
    @DisplayName("markFailed에 null 메시지를 주면 lastError는 null로 유지된다")
    void markFailedWithNullKeepsLastErrorNull() {
        DwIngestionOutbox outbox = DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, CLOCK);

        outbox.markFailed(CLOCK, null);

        assertThat(outbox.getLastError()).isNull();
    }

    @Test
    @DisplayName("markSending(잠금 정보 없음)은 잠금자를 null로 둔다")
    void markSendingWithoutLocker() {
        DwIngestionOutbox outbox = DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, CLOCK);

        outbox.markSending(CLOCK);

        assertThat(outbox.getLockedBy()).isNull();
    }

    @Test
    @DisplayName("markRetry에 0초 지연을 주면 availableAt이 현재 시간으로 설정되고 retryCount가 증가한다")
    void markRetryWithZeroDelay() {
        DwIngestionOutbox outbox = DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, CLOCK);

        outbox.markRetry(CLOCK, Duration.ZERO, "retry now");

        assertThat(outbox.getAvailableAt()).isEqualTo(OffsetDateTime.now(CLOCK));
        assertThat(outbox.getRetryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("payload가 null일 때 그대로 유지된다")
    void payloadRemainsNull() {
        DwIngestionOutbox outbox = DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, CLOCK);

        assertThat(outbox.getPayload()).isNull();
    }

    @Test
    @DisplayName("markDeadLetter에 null 메시지를 주면 lastError는 null로 유지된다")
    void markDeadLetterWithNullLeavesErrorNull() {
        DwIngestionOutbox outbox = DwIngestionOutbox.pending(DwIngestionJobType.FETCH_NEXT, CLOCK);

        outbox.markDeadLetter(CLOCK, null);

        assertThat(outbox.getLastError()).isNull();
        assertThat(outbox.getStatus()).isEqualTo(DwIngestionOutboxStatus.DEAD_LETTER);
    }
}
