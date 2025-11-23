package com.example.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.masking.MaskingTarget;

class AuditPortDefaultTest {

    static class TestPort implements AuditPort {
        boolean called;
        @Override public void record(AuditEvent event, AuditMode mode) { called = true; }
        @Override public Optional<AuditPolicySnapshot> resolve(String endpoint, String eventType) { return Optional.empty(); }
    }

    @Test
    @DisplayName("AuditPort default record(maskingTarget) 오버로드가 기본 record로 위임한다")
    void defaultRecordOverloadDelegates() {
        TestPort port = new TestPort();
        AuditEvent event = AuditEvent.builder().eventType("T").build();
        assertThatCode(() -> port.record(event, AuditMode.ASYNC_FALLBACK, MaskingTarget.builder().build()))
                .doesNotThrowAnyException();
        assertThat(port.called).isTrue();
    }
}
