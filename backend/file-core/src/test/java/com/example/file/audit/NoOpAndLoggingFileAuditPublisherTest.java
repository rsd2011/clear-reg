package com.example.file.audit;

import static org.assertj.core.api.Assertions.assertThatNoException;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NoOpAndLoggingFileAuditPublisherTest {

    private static final FileAuditEvent EVENT = new FileAuditEvent(
            "UPLOAD", UUID.randomUUID(), "tester", OffsetDateTime.parse("2024-02-01T00:00:00Z"));

    @Test
    @DisplayName("NoOp 퍼블리셔는 이벤트를 받아도 예외 없이 종료한다")
    void noOpPublisherDoesNothing() {
        NoOpFileAuditPublisher publisher = new NoOpFileAuditPublisher();

        assertThatNoException().isThrownBy(() -> publisher.publish(EVENT));
    }

    @Test
    @DisplayName("로깅 퍼블리셔는 이벤트를 로그로 남기고 예외를 발생시키지 않는다")
    void loggingPublisherLogs() {
        LoggingFileAuditPublisher publisher = new LoggingFileAuditPublisher();

        assertThatNoException().isThrownBy(() -> publisher.publish(EVENT));
    }
}
