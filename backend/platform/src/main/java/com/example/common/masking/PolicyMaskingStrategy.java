package com.example.common.masking;

import java.util.EnumMap;
import java.util.Map;

import com.example.common.policy.PolicyToggleSettings;

/**
 * PolicyToggleSettings 기반 기본 구현.
 * - 고객(CUSTOMER)은 정책 플래그를 따름
 * - 직원/시스템은 maskingEmployeeEnabled 플래그를 따름(기본 false)
 */
public class PolicyMaskingStrategy implements MaskingStrategy {

    private final Map<SubjectType, Boolean> maskBySubject;
    private final java.util.Set<String> allowedUnmaskRoles;
    private final boolean defaultMask;

    /**
     * 기본 생성자: CUSTOMER는 정책 플래그, 나머지는 기본 false.
     */
    public PolicyMaskingStrategy(PolicyToggleSettings settings) {
        this(settings, defaultRules(settings), java.util.Set.of());
    }

    /**
     * 주입형 생성자: SubjectType별 마스킹 여부를 자유롭게 설정 가능.
     */
    public PolicyMaskingStrategy(PolicyToggleSettings settings,
                                 Map<SubjectType, Boolean> maskBySubject,
                                 java.util.Set<String> allowedUnmaskRoles) {
        this.defaultMask = settings.auditMaskingEnabled();
        EnumMap<SubjectType, Boolean> map = new EnumMap<>(SubjectType.class);
        map.putAll(maskBySubject);
        this.maskBySubject = Map.copyOf(map);
        this.allowedUnmaskRoles = allowedUnmaskRoles == null ? java.util.Set.of() : java.util.Set.copyOf(allowedUnmaskRoles);
    }

    private static Map<SubjectType, Boolean> defaultRules(PolicyToggleSettings settings) {
        EnumMap<SubjectType, Boolean> map = new EnumMap<>(SubjectType.class);
        map.put(SubjectType.CUSTOMER_INDIVIDUAL, settings.auditMaskingEnabled());
        map.put(SubjectType.CUSTOMER_CORPORATE, settings.auditMaskingEnabled());
        map.put(SubjectType.EMPLOYEE, false);
        map.put(SubjectType.SYSTEM, false);
        map.put(SubjectType.UNKNOWN, settings.auditMaskingEnabled());
        return map;
    }

    @Override
    public boolean shouldMask(MaskingTarget target) {
        boolean base = target != null ? target.isDefaultMask() : defaultMask;
        if (target != null && target.getRequesterRoles() != null) {
            boolean allowed = target.getRequesterRoles().stream().anyMatch(allowedUnmaskRoles::contains);
            if (allowed && (target.isForceUnmask()
                    || (target.getForceUnmaskKinds() != null && target.getForceUnmaskKinds().contains(target.getDataKind()))
                    || (target.getForceUnmaskFields() != null && !target.getForceUnmaskFields().isEmpty()))) {
                return false;
            }
        }
        if (target == null || target.getSubjectType() == null) {
            return base;
        }
        return maskBySubject.getOrDefault(target.getSubjectType(), base);
    }
}
