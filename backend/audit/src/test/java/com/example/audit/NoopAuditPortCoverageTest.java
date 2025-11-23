package com.example.audit;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NoopAuditPortCoverageTest {

    @Test
    @DisplayName("NoopAuditPort는 record/resolve 호출 시 예외를 던지지 않는다")
    void noopDoesNothing() {
        NoopAuditPort port = new NoopAuditPort();
        assertThatCode(() -> port.record(AuditEvent.builder().eventType("T").build(), AuditMode.ASYNC_FALLBACK)).doesNotThrowAnyException();
        Optional<AuditPolicySnapshot> resolved = port.resolve("/api", "TYPE");
        assertThatCode(resolved::isEmpty).doesNotThrowAnyException();
    }
}
