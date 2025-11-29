package com.example.common.policy;

import java.util.UUID;

import com.example.common.security.RowScope;

import lombok.Builder;
import lombok.Value;

/**
 * 행 수준 접근 정책 매칭 결과.
 */
@Value
@Builder
public class RowAccessMatch {
    UUID policyId;
    RowScope rowScope;
    Integer priority;
}
