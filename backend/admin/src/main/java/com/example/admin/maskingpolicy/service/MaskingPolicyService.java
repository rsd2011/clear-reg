package com.example.admin.maskingpolicy.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.admin.maskingpolicy.domain.MaskingPolicy;
import com.example.admin.maskingpolicy.repository.MaskingPolicyRepository;
import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.common.masking.DataKind;
import com.example.common.policy.MaskingMatch;

/**
 * 마스킹 정책 평가 서비스.
 * <p>
 * 새로운 버전 기반 구조(MaskingPolicy)를 사용하여 마스킹 정책을 평가합니다.
 */
@Service
public class MaskingPolicyService {

    private final MaskingPolicyRepository versionRepository;

    public MaskingPolicyService(MaskingPolicyRepository versionRepository) {
        this.versionRepository = versionRepository;
    }

    @Transactional(readOnly = true)
    public Optional<MaskingMatch> evaluate(String featureCode,
                                           String actionCode,
                                           String permGroupCode,
                                           List<String> orgGroupCodes,
                                           DataKind dataKind,
                                           Instant now) {
        // 활성화된 현재 버전들 조회 (우선순위 순)
        List<MaskingPolicy> candidates = versionRepository.findAllCurrentActiveVersions();
        Instant ts = now != null ? now : Instant.now();

        FeatureCode feature = parseFeatureCode(featureCode);
        ActionCode action = parseActionCode(actionCode);

        if (feature == null) {
            return Optional.empty();
        }

        return candidates.stream()
                .filter(v -> isEffective(v, ts))
                .filter(v -> v.getFeatureCode() == feature)
                .filter(v -> matchAction(action, v.getActionCode()))
                .filter(v -> matchNullable(permGroupCode, v.getPermGroupCode()))
                .filter(v -> matchGroup(orgGroupCodes, v.getOrgGroupCode()))
                .filter(v -> matchDataKinds(dataKind, v.getDataKinds()))
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

    private boolean isEffective(MaskingPolicy v, Instant ts) {
        return (v.getEffectiveFrom() == null || !ts.isBefore(v.getEffectiveFrom()))
                && (v.getEffectiveTo() == null || ts.isBefore(v.getEffectiveTo()));
    }

    private boolean matchNullable(String value, String target) {
        if (target == null || target.isBlank()) return true;
        return value != null && value.equalsIgnoreCase(target);
    }

    private boolean matchGroup(List<String> groupCodes, String target) {
        if (target == null || target.isBlank()) return true;
        return groupCodes != null && groupCodes.stream().anyMatch(target::equalsIgnoreCase);
    }

    /**
     * DataKind 매칭 로직.
     * 정책의 dataKinds가 비어있으면 모든 종류에 적용됨.
     */
    private boolean matchDataKinds(DataKind queryKind, Set<DataKind> policyKinds) {
        // 정책의 dataKinds가 비어있으면 모든 종류에 적용
        if (policyKinds == null || policyKinds.isEmpty()) {
            return true;
        }
        return queryKind != null && policyKinds.contains(queryKind);
    }

    private MaskingMatch toMatch(MaskingPolicy v) {
        return MaskingMatch.builder()
                .policyId(v.getRoot().getId())
                .dataKinds(v.getDataKinds())
                .maskingEnabled(Boolean.TRUE.equals(v.getMaskingEnabled()))
                .auditEnabled(Boolean.TRUE.equals(v.getAuditEnabled()))
                .priority(v.getPriority())
                .build();
    }
}
