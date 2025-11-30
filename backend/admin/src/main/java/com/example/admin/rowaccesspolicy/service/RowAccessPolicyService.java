package com.example.admin.rowaccesspolicy.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.admin.rowaccesspolicy.domain.RowAccessPolicy;
import com.example.admin.rowaccesspolicy.repository.RowAccessPolicyRepository;
import com.example.common.policy.RowAccessMatch;

/**
 * 행 접근 정책 평가 서비스.
 * <p>
 * 새로운 버전 기반 구조(RowAccessPolicy)를 사용하여 행 접근 정책을 평가합니다.
 */
@Service
public class RowAccessPolicyService {

    private final RowAccessPolicyRepository versionRepository;

    public RowAccessPolicyService(RowAccessPolicyRepository versionRepository) {
        this.versionRepository = versionRepository;
    }

    /**
     * 행 접근 정책을 평가합니다.
     *
     * @param featureCode 기능 코드
     * @param actionCode 액션 코드
     * @param permGroupCode 권한 그룹 코드
     * @param orgGroupCodes 조직 그룹 코드 목록
     * @param now 평가 시점 (null이면 현재 시점 사용)
     * @return 매칭된 정책 결과
     */
    @Transactional(readOnly = true)
    public Optional<RowAccessMatch> evaluate(String featureCode,
                                             String actionCode,
                                             String permGroupCode,
                                             List<String> orgGroupCodes,
                                             Instant now) {
        // 활성화된 현재 버전들 조회 (우선순위 순)
        List<RowAccessPolicy> candidates = versionRepository.findAllCurrentActiveVersions();
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

    private boolean isEffective(RowAccessPolicy v, Instant ts) {
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

    private RowAccessMatch toMatch(RowAccessPolicy v) {
        return RowAccessMatch.builder()
                .policyId(v.getRoot().getId())
                .rowScope(v.getRowScope())
                .priority(v.getPriority())
                .build();
    }
}
