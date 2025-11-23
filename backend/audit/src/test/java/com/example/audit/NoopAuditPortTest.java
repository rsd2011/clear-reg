package com.example.audit;

import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NoopAuditPort는 예외 없이 동작")
class NoopAuditPortTest {

    @Test
    void noExceptions() {
        NoopAuditPort port = new NoopAuditPort();
        assertThatNoException().isThrownBy(() -> port.record(null, AuditMode.ASYNC_FALLBACK));
        assertThatNoException().isThrownBy(() -> port.record(null, AuditMode.STRICT, null));
        assertThatNoException().isThrownBy(() -> port.resolve("/endpoint", "LOGIN"));
        Optional<AuditPolicySnapshot> resolved = port.resolve("/e", "t");
        assertThatNoException().isThrownBy(resolved::isEmpty);
    }
}

