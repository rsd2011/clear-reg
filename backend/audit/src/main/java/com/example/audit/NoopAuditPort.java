package com.example.audit;

import java.util.Optional;

/** 기본 안전 동작: 감사 실패나 미구현 시에도 업무 흐름을 막지 않는다. */
public class NoopAuditPort implements AuditPort {

    @Override
    public void record(AuditEvent event, AuditMode mode) {
        // no-op
    }

    @Override
    public Optional<AuditPolicySnapshot> resolve(String endpoint, String eventType) {
        return Optional.empty();
    }
}
