package com.example.audit;

import java.util.Map;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

/** 정책 조회 결과를 런타임에 캐시하기 위한 스냅샷. */
@Value
@Builder(toBuilder = true)
public class AuditPolicySnapshot {
    @Builder.Default
    boolean enabled = true;

    @Builder.Default
    boolean sensitiveApi = false;

    @Builder.Default
    boolean reasonRequired = true;

    @Builder.Default
    boolean maskingEnabled = true;

    @Builder.Default
    AuditMode mode = AuditMode.STRICT;

    Integer retentionDays;

    @Builder.Default
    RiskLevel riskLevel = RiskLevel.MEDIUM;

    @Singular
    Map<String, Object> attributes;

    public static AuditPolicySnapshot secureDefault() {
        return AuditPolicySnapshot.builder().build();
    }
}
