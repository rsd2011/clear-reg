package com.example.audit;

import java.util.Optional;

import com.example.common.masking.MaskingTarget;

/**
 * 업무 모듈이 의존하는 감사 포트. 구현체는 infra 레이어에서 제공한다.
 */
public interface AuditPort {

    /** 감사 이벤트 기록. */
    void record(AuditEvent event, AuditMode mode);

    /** 마스킹 컨텍스트를 포함한 감사 이벤트 기록. */
    default void record(AuditEvent event, AuditMode mode, MaskingTarget maskingTarget) {
        record(event, mode);
    }

    /** 엔드포인트/이벤트 타입별 정책 해석 결과. */
    Optional<AuditPolicySnapshot> resolve(String endpoint, String eventType);
}
