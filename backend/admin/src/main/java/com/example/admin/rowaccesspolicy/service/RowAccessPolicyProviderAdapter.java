package com.example.admin.rowaccesspolicy.service;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.common.policy.RowAccessMatch;
import com.example.common.policy.RowAccessPolicyProvider;
import com.example.common.policy.RowAccessQuery;

@Component
public class RowAccessPolicyProviderAdapter implements RowAccessPolicyProvider {

    private final RowAccessPolicyService service;

    public RowAccessPolicyProviderAdapter(RowAccessPolicyService service) {
        this.service = service;
    }

    @Override
    public Optional<RowAccessMatch> evaluate(RowAccessQuery query) {
        return service.evaluate(
                query.featureCode(),
                query.actionCode(),
                query.permGroupCode(),
                query.orgGroupCodes(),
                query.nowOrDefault());
    }
}
