package com.example.common.policy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DataPolicyMatch {
    java.util.UUID policyId;
    String rowScope;
    String rowScopeExpr;
    String maskRule;
    String maskParams;
    Integer priority;
}
