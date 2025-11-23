package com.example.audit.infra.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.audit.AuditMode;
import com.example.audit.RiskLevel;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;

@DisplayName("AuditPolicyResolver 캐시/리스크레벨 분기")
class AuditPolicyResolverTest {

    @Test
    void returnsSecureDefaultWhenNoProvider() {
        AuditPolicyResolver resolver = new AuditPolicyResolver(null);
        Optional<?> snap = resolver.resolve("/endpoint", "LOGIN");
        assertThat(snap).isPresent();
        resolver.invalidateAll(); // 브랜치 커버
    }

    @Test
    void mapsSettingsAndRiskLevels() {
        PolicyToggleSettings settings = new PolicyToggleSettings(
                true, true, true, List.of(), 10, List.of(), true, 30,
                true, true, true, 365, false, "HIGH", true, List.of("/s"), List.of("AUDIT"));
        PolicySettingsProvider provider = () -> settings;
        AuditPolicyResolver resolver = new AuditPolicyResolver(provider);

        var snap = resolver.resolve("/e", "T").orElseThrow();
        assertThat(snap.getMode()).isEqualTo(AuditMode.ASYNC_FALLBACK);
        assertThat(snap.getRiskLevel()).isEqualTo(RiskLevel.HIGH);

        // invalid risk level -> MEDIUM
        PolicySettingsProvider bad = () -> new PolicyToggleSettings(true, true, true, List.of(), 10, List.of(), true, 30,
                true, true, true, 365, true, "INVALID", true, List.of(), List.of());
        AuditPolicyResolver resolverBad = new AuditPolicyResolver(bad);
        assertThat(resolverBad.resolve("/e", "T").orElseThrow().getRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
    }
}

