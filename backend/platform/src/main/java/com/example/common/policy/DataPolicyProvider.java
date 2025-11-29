package com.example.common.policy;

import java.util.Optional;

/**
 * @deprecated RowAccessPolicyProvider와 MaskingPolicyProvider를 사용하세요.
 */
@Deprecated(forRemoval = true)
public interface DataPolicyProvider {
    Optional<DataPolicyMatch> evaluate(DataPolicyQuery query);
}
