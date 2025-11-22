package com.example.file.audit;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DisabledAndNoOpAuditPublisherTest {

    @Test
    @DisplayName("NoOp 퍼블리셔는 이벤트를 무시하고 성공한다")
    void noOpPublisherCompletesSilently() {
        FileAuditPublisher publisher = new NoOpFileAuditPublisher();

        assertThatCode(() -> publisher.publish(sampleEvent())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("로깅 퍼블리셔는 예외를 던지지 않는다")
    void loggingPublisherDoesNotThrow() {
        FileAuditPublisher publisher = new LoggingFileAuditPublisher();

        assertThatCode(() -> publisher.publish(sampleEvent())).doesNotThrowAnyException();
    }

    private FileAuditEvent sampleEvent() {
        return new FileAuditEvent("UPLOAD", UUID.randomUUID(), "actor", java.time.OffsetDateTime.now());
    }
}
