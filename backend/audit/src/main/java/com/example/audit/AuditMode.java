package com.example.audit;

import com.example.common.codegroup.annotation.ManagedCode;

/** 감사 기록 처리 모드. */
@ManagedCode
public enum AuditMode {
    /** 실패 시 업무 트랜잭션을 함께 롤백. */
    STRICT,
    /** 실패를 DLQ/재시도로 넘기고 본 업무는 진행. */
    ASYNC_FALLBACK
}
