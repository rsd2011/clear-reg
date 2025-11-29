package com.example.admin.rowaccesspolicy.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.admin.rowaccesspolicy.domain.RowAccessPolicy;
import com.example.admin.rowaccesspolicy.repository.RowAccessPolicyRepository;
import com.example.common.policy.RowAccessMatch;

@Service
public class RowAccessPolicyService {

    private final RowAccessPolicyRepository repository;

    public RowAccessPolicyService(RowAccessPolicyRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<RowAccessMatch> evaluate(String featureCode,
                                             String actionCode,
                                             String permGroupCode,
                                             List<String> orgGroupCodes,
                                             Instant now) {
        List<RowAccessPolicy> candidates = repository.findByActiveTrueOrderByPriorityAsc();
        Instant ts = now != null ? now : Instant.now();

        FeatureCode feature = parseFeatureCode(featureCode);
        ActionCode action = parseActionCode(actionCode);

        if (feature == null) {
            return Optional.empty();
        }

        return candidates.stream()
                .filter(p -> isEffective(p, ts))
                .filter(p -> p.getFeatureCode() == feature)
                .filter(p -> matchAction(action, p.getActionCode()))
                .filter(p -> matchNullable(permGroupCode, p.getPermGroupCode()))
                .filter(p -> matchGroup(orgGroupCodes, p.getOrgGroupCode()))
                .min(Comparator.comparing(RowAccessPolicy::getPriority))
                .map(this::toMatch);
    }

    private FeatureCode parseFeatureCode(String code) {
        if (code == null || code.isBlank()) return null;
        try {
            return FeatureCode.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private ActionCode parseActionCode(String code) {
        if (code == null || code.isBlank()) return null;
        try {
            return ActionCode.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean matchAction(ActionCode value, ActionCode target) {
        if (target == null) return true;
        return value != null && value == target;
    }

    private boolean isEffective(RowAccessPolicy p, Instant ts) {
        return (p.getEffectiveFrom() == null || !ts.isBefore(p.getEffectiveFrom()))
                && (p.getEffectiveTo() == null || ts.isBefore(p.getEffectiveTo()));
    }

    private boolean matchNullable(String value, String target) {
        if (target == null || target.isBlank()) return true;
        return value != null && value.equalsIgnoreCase(target);
    }

    private boolean matchGroup(List<String> groupCodes, String target) {
        if (target == null || target.isBlank()) return true;
        return groupCodes != null && groupCodes.stream().anyMatch(target::equalsIgnoreCase);
    }

    private RowAccessMatch toMatch(RowAccessPolicy p) {
        return RowAccessMatch.builder()
                .policyId(p.getId())
                .rowScope(p.getRowScope())
                .priority(p.getPriority())
                .build();
    }
}
