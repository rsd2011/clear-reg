package com.example.common.policy;

import java.util.Optional;

/**
 * 행 수준 접근 정책 제공자.
 */
public interface RowAccessPolicyProvider {
    Optional<RowAccessMatch> evaluate(RowAccessQuery query);
}
