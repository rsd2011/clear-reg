package com.example.batch.audit;

/**
 * 정책 변경 시 AuditPartitionScheduler가 즉시 설정을 재적용하도록 알리는 이벤트.
 */
public record AuditPartitionPolicyChangedEvent(String code) {
    public static final String AUDIT_POLICY_CODE = "security.policy";
}
