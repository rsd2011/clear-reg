package com.example.common.policy;

import java.util.Optional;

public interface DataPolicyProvider {
    Optional<DataPolicyMatch> evaluate(DataPolicyQuery query);
}
