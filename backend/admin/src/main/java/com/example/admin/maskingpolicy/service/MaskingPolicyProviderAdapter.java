package com.example.admin.maskingpolicy.service;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.common.policy.MaskingMatch;
import com.example.common.policy.MaskingPolicyProvider;
import com.example.common.policy.MaskingQuery;

@Component
public class MaskingPolicyProviderAdapter implements MaskingPolicyProvider {

    private final MaskingPolicyService service;

    public MaskingPolicyProviderAdapter(MaskingPolicyService service) {
        this.service = service;
    }

    @Override
    public Optional<MaskingMatch> evaluate(MaskingQuery query) {
        return service.evaluate(
                query.featureCode(),
                query.actionCode(),
                query.permGroupCode(),
                query.orgGroupCodes(),
                query.dataKind(),
                query.nowOrDefault());
    }
}
