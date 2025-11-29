package com.example.common.policy;

import java.util.Optional;

/**
 * 마스킹 정책 제공자.
 */
public interface MaskingPolicyProvider {
    Optional<MaskingMatch> evaluate(MaskingQuery query);
}
