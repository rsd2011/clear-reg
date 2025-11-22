package com.example.audit.infra.policy;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.example.audit.AuditMode;
import com.example.audit.AuditPolicySnapshot;
import com.example.audit.RiskLevel;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/** 정책을 조회하고 Secure-by-default 캐시를 제공한다. */
@Component
public class AuditPolicyResolver {

    private final PolicySettingsProvider policySettingsProvider;
    private final Cache<PolicyKey, AuditPolicySnapshot> cache;

    @Autowired
    public AuditPolicyResolver(@Nullable PolicySettingsProvider policySettingsProvider) {
        this.policySettingsProvider = policySettingsProvider;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .maximumSize(1_000)
                .build();
    }

    public Optional<AuditPolicySnapshot> resolve(String endpoint, String eventType) {
        PolicyKey key = new PolicyKey(endpoint, eventType);
        AuditPolicySnapshot snapshot = cache.get(key, k -> loadSnapshot());
        return Optional.ofNullable(snapshot);
    }

    public void invalidateAll() {
        cache.invalidateAll();
    }

    private AuditPolicySnapshot loadSnapshot() {
        if (policySettingsProvider == null) {
            return AuditPolicySnapshot.secureDefault();
        }
        PolicyToggleSettings settings = policySettingsProvider.currentSettings();
        return AuditPolicySnapshot.builder()
                .enabled(settings.auditEnabled())
                .sensitiveApi(settings.auditSensitiveApiDefaultOn())
                .reasonRequired(settings.auditReasonRequired())
                .mode(settings.auditStrictMode() ? AuditMode.STRICT : AuditMode.ASYNC_FALLBACK)
                .retentionDays(settings.auditRetentionDays())
                .riskLevel(toRiskLevel(settings.auditRiskLevel()))
                .build();
    }

    private RiskLevel toRiskLevel(String riskLevel) {
        if (riskLevel == null || riskLevel.isBlank()) {
            return RiskLevel.MEDIUM;
        }
        try {
            return RiskLevel.valueOf(riskLevel.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return RiskLevel.MEDIUM;
        }
    }

    private record PolicyKey(String endpoint, String eventType) {
    }
}
