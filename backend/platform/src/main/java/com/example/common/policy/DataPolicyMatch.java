package com.example.common.policy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DataPolicyMatch {
    java.util.UUID policyId;
    String sensitiveTag;
    String rowScope;
    String rowScopeExpr;
    String maskRule;
    String maskParams;
    String requiredActionCode;
    boolean auditEnabled;
    Integer priority;
}
