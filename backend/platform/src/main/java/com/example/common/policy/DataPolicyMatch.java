package com.example.common.policy;

import com.example.common.security.RowScope;

import lombok.Builder;
import lombok.Value;

/**
 * @deprecated RowAccessMatch와 MaskingMatch를 사용하세요.
 */
@Deprecated(forRemoval = true)
@Value
@Builder
public class DataPolicyMatch {
    java.util.UUID policyId;
    RowScope rowScope;
    String maskRule;
    String maskParams;
    boolean auditEnabled;
    Integer priority;
}
