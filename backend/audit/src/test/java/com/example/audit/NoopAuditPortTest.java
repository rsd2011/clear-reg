package com.example.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NoopAuditPortTest {

    @Test
    void record_doesNotThrow() {
        var port = new NoopAuditPort();
        port.record(AuditEvent.builder().eventType("TEST").build(), AuditMode.STRICT);
    }

    @Test
    void resolve_returnsEmpty() {
        var port = new NoopAuditPort();
        assertThat(port.resolve("/test", "TYPE")).isEmpty();
    }
}
