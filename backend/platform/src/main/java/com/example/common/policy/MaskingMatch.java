package com.example.common.policy;

import java.util.UUID;

import lombok.Builder;
import lombok.Value;

/**
 * 마스킹 정책 매칭 결과.
 */
@Value
@Builder
public class MaskingMatch {
    UUID policyId;
    String dataKind;
    String maskRule;
    String maskParams;
    boolean auditEnabled;
    Integer priority;
}
