package com.example.admin.maskingpolicy.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.admin.maskingpolicy.domain.MaskingPolicy;
import com.example.admin.maskingpolicy.repository.MaskingPolicyRepository;
import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.common.policy.MaskingMatch;

@Service
public class MaskingPolicyService {

    private final MaskingPolicyRepository repository;

    public MaskingPolicyService(MaskingPolicyRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<MaskingMatch> evaluate(String featureCode,
                                           String actionCode,
                                           String permGroupCode,
                                           List<String> orgGroupCodes,
                                           String dataKind,
                                           Instant now) {
        List<MaskingPolicy> candidates = repository.findByActiveTrueOrderByPriorityAsc();
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
                .filter(p -> matchDataKind(dataKind, p.getDataKind()))
                .min(Comparator.comparing(MaskingPolicy::getPriority))
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

    private boolean isEffective(MaskingPolicy p, Instant ts) {
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

    private boolean matchDataKind(String value, String target) {
        // 정책의 dataKind가 null이면 모든 종류에 적용
        if (target == null || target.isBlank()) return true;
        return value != null && value.equalsIgnoreCase(target);
    }

    private MaskingMatch toMatch(MaskingPolicy p) {
        return MaskingMatch.builder()
                .policyId(p.getId())
                .dataKind(p.getDataKind())
                .maskRule(p.getMaskRule())
                .maskParams(p.getMaskParams())
                .auditEnabled(Boolean.TRUE.equals(p.getAuditEnabled()))
                .priority(p.getPriority())
                .build();
    }
}
