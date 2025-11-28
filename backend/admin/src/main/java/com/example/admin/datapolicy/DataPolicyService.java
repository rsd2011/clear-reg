package com.example.admin.datapolicy;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.common.policy.DataPolicyMatch;

@Service
public class DataPolicyService {

    private final DataPolicyRepository repository;

    public DataPolicyService(DataPolicyRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<DataPolicyMatch> evaluate(String featureCode,
                                              String actionCode,
                                              String permGroupCode,
                                              Long orgPolicyId,
                                              List<String> orgGroupCodes,
                                              String businessType,
                                              String sensitiveTag,
                                              Instant now) {
        List<DataPolicy> candidates = repository.findByActiveTrueOrderByPriorityAsc();
        Instant ts = now != null ? now : Instant.now();
        return candidates.stream()
                .filter(p -> isEffective(p, ts))
                .filter(p -> match(featureCode, p.getFeatureCode()))
                .filter(p -> matchNullable(actionCode, p.getActionCode()))
                .filter(p -> matchNullable(permGroupCode, p.getPermGroupCode()))
                .filter(p -> matchNullable(orgPolicyId, p.getOrgPolicyId()))
                .filter(p -> matchGroup(orgGroupCodes, p.getOrgGroupCode()))
                .filter(p -> matchNullable(businessType, p.getBusinessType()))
                .filter(p -> matchSensitiveTag(sensitiveTag, p.getSensitiveTag()))
                .min(Comparator.comparing(DataPolicy::getPriority))
                .map(this::toMatch);
    }

    /**
     * sensitiveTag 매칭 로직:
     * - policy.sensitiveTag가 null이면 해당 feature/action의 기본 정책 (모든 태그에 적용 가능)
     * - policy.sensitiveTag가 있으면 요청 태그와 정확히 일치해야 함
     */
    private boolean matchSensitiveTag(String requestTag, String policyTag) {
        if (policyTag == null || policyTag.isBlank()) {
            return true; // 기본 정책은 모든 태그에 매칭
        }
        return requestTag != null && requestTag.equalsIgnoreCase(policyTag);
    }

    private boolean isEffective(DataPolicy p, Instant ts) {
        return (p.getEffectiveFrom() == null || !ts.isBefore(p.getEffectiveFrom()))
                && (p.getEffectiveTo() == null || ts.isBefore(p.getEffectiveTo()));
    }

    private boolean match(String value, String target) {
        return value != null && value.equalsIgnoreCase(target);
    }

    private boolean matchNullable(String value, String target) {
        if (target == null || target.isBlank()) return true;
        return value != null && value.equalsIgnoreCase(target);
    }

    private boolean matchNullable(Long value, Long target) {
        if (target == null) return true;
        return value != null && value.equals(target);
    }

    private boolean matchGroup(List<String> groupCodes, String target) {
        if (target == null || target.isBlank()) return true;
        return groupCodes != null && groupCodes.stream().anyMatch(target::equalsIgnoreCase);
    }

    private DataPolicyMatch toMatch(DataPolicy p) {
        return DataPolicyMatch.builder()
                .policyId(p.getId())
                .sensitiveTag(p.getSensitiveTag())
                .rowScope(p.getRowScope())
                .rowScopeExpr(p.getRowScopeExpr())
                .maskRule(p.getDefaultMaskRule())
                .maskParams(p.getMaskParams())
                .requiredActionCode(p.getRequiredActionCode())
                .auditEnabled(Boolean.TRUE.equals(p.getAuditEnabled()))
                .priority(p.getPriority())
                .build();
    }
}
