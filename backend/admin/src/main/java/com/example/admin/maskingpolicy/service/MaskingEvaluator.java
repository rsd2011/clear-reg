package com.example.admin.maskingpolicy.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.admin.permission.domain.ActionCode;
import com.example.common.policy.MaskingMatch;
import com.example.common.policy.MaskingPolicyProvider;
import com.example.common.policy.MaskingQuery;
import com.example.common.security.CurrentUser;
import com.example.common.security.CurrentUserProvider;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;

/**
 * MaskingPolicy 기반 마스킹 평가기.
 *
 * <p>CurrentUser의 컨텍스트(feature, action, permGroup 등)를 기반으로
 * MaskingPolicy를 조회하여 마스킹 규칙을 적용합니다.
 */
@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Uses immutable snapshot of CurrentUser; no internal mutable state exposed")
@Component
@Slf4j
public class MaskingEvaluator {

    private static final String DEFAULT_MASK = "***";

    private final CurrentUserProvider currentUserProvider;
    private final MaskingPolicyProvider maskingPolicyProvider;

    public MaskingEvaluator(CurrentUserProvider currentUserProvider,
                            MaskingPolicyProvider maskingPolicyProvider) {
        this.currentUserProvider = currentUserProvider;
        this.maskingPolicyProvider = maskingPolicyProvider;
    }

    /**
     * 지정된 dataKind의 값을 MaskingPolicy 기반으로 마스킹 평가합니다.
     *
     * @param dataKind 민감 데이터 종류 (SSN, PHONE, EMAIL 등)
     * @param rawValue 원본 값
     * @return 마스킹된 값 또는 원본 값
     */
    public Object mask(String dataKind, Object rawValue) {
        Optional<CurrentUser> maybeUser = currentUserProvider.current();
        if (maybeUser.isEmpty()) {
            log.warn(
                "MASKING_WITHOUT_CONTEXT dataKind={} valueType={}",
                dataKind,
                rawValue != null ? rawValue.getClass().getName() : "null");
            return maskValue(rawValue, DEFAULT_MASK);
        }

        CurrentUser user = maybeUser.get();

        // MaskingPolicy 조회
        MaskingQuery query = new MaskingQuery(
            user.featureCode(),
            user.actionCode(),
            user.permissionGroupCode(),
            user.orgGroupCodes(),
            dataKind,
            Instant.now()
        );

        Optional<MaskingMatch> maybePolicy = maskingPolicyProvider.evaluate(query);

        if (maybePolicy.isEmpty()) {
            // 매칭되는 정책이 없으면 기본 마스킹
            log.debug("NO_MASKING_POLICY_MATCH dataKind={} user={}", dataKind, user.username());
            return maskValue(rawValue, DEFAULT_MASK);
        }

        MaskingMatch policy = maybePolicy.get();

        // NONE 규칙이면 마스킹 없이 원본 반환
        if ("NONE".equalsIgnoreCase(policy.getMaskRule())) {
            return rawValue;
        }

        // ActionCode.canUnmask()로 언마스킹 가능 여부 판단
        ActionCode currentAction = parseAction(user.actionCode(), ActionCode.READ);

        if (currentAction.canUnmask()) {
            if (policy.isAuditEnabled()) {
                log.info(
                    "UNMASK_GRANTED user={} dataKind={} feature={} action={} policyId={}",
                    user.username(),
                    dataKind,
                    user.featureCode(),
                    user.actionCode(),
                    policy.getPolicyId());
            }
            return rawValue;
        }

        if (policy.isAuditEnabled()) {
            log.debug(
                "MASK_APPLIED user={} dataKind={} feature={} action={} policyId={}",
                user.username(),
                dataKind,
                user.featureCode(),
                user.actionCode(),
                policy.getPolicyId());
        }

        return applyMaskRule(rawValue, policy);
    }

    /**
     * 마스킹 규칙에 따라 값을 마스킹합니다.
     */
    private Object applyMaskRule(Object rawValue, MaskingMatch policy) {
        String maskRule = policy.getMaskRule();

        switch (maskRule.toUpperCase()) {
            case "FULL":
                return maskValue(rawValue, DEFAULT_MASK);
            case "PARTIAL":
                return partialMask(rawValue);
            case "HASH":
                return hashMask(rawValue);
            case "TOKENIZE":
                return hashMask(rawValue); // 토큰화는 해시로 대체
            default:
                return maskValue(rawValue, DEFAULT_MASK);
        }
    }

    private Object partialMask(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        String str = rawValue.toString();
        if (str.length() <= 4) {
            return DEFAULT_MASK;
        }
        // 기본: 앞 2자리, 뒤 2자리 노출
        int showStart = 2;
        int showEnd = 2;
        return str.substring(0, showStart) + "***" + str.substring(str.length() - showEnd);
    }

    private Object hashMask(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        // SHA-256 해시의 앞 8자리 반환
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(rawValue.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4 && i < hash.length; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return DEFAULT_MASK;
        }
    }

    private ActionCode parseAction(String code, ActionCode fallback) {
        if (code == null || code.isBlank()) {
            return fallback;
        }
        try {
            return ActionCode.valueOf(code.trim());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private Object maskValue(Object rawValue, String maskWith) {
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof Number) {
            return 0;
        }
        if (rawValue instanceof Boolean) {
            return false;
        }
        return maskWith;
    }
}
