package com.example.admin.masking;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.common.policy.DataPolicyProvider;
import com.example.common.policy.DataPolicyQuery;
import com.example.common.policy.DataPolicyMatch;

@Component
public class DataPolicyProviderAdapter implements DataPolicyProvider {

    private final DataPolicyService service;

    public DataPolicyProviderAdapter(DataPolicyService service) {
        this.service = service;
    }

    @Override
    public Optional<DataPolicyMatch> evaluate(DataPolicyQuery query) {
        return service.evaluate(query.featureCode(),
                query.actionCode(),
                query.permGroupCode(),
                query.orgPolicyId(),
                query.orgGroupCodes(),
                query.businessType(),
                query.sensitiveTag(),
                query.nowOrDefault());
    }
}
