package com.example.audit.infra.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;

class AuditPolicyResolverTest {

    @Test
    void resolve_returnsSecureDefaultWhenProviderMissing() {
        AuditPolicyResolver resolver = new AuditPolicyResolver(null);

        var snapshot = resolver.resolve("/endpoint", "TYPE").orElseThrow();

        assertThat(snapshot.isEnabled()).isTrue();
        assertThat(snapshot.isReasonRequired()).isTrue();
        assertThat(snapshot.getMode()).isNotNull();
    }

    @Test
    void resolve_appliesProviderValues() {
        PolicySettingsProvider provider = () -> new PolicyToggleSettings(true, true, true,
                java.util.List.of(), 1L, java.util.List.of(), true, 30,
                false, false, true, 90, false, "HIGH");

        AuditPolicyResolver resolver = new AuditPolicyResolver(provider);

        var snapshot = resolver.resolve("/endpoint", "TYPE").orElseThrow();

        assertThat(snapshot.isEnabled()).isFalse();
        assertThat(snapshot.isReasonRequired()).isFalse();
        assertThat(snapshot.isSensitiveApi()).isTrue();
        assertThat(snapshot.getMode().name()).isEqualTo("ASYNC_FALLBACK");
        assertThat(snapshot.getRetentionDays()).isEqualTo(90);
        assertThat(snapshot.getRiskLevel().name()).isEqualTo("HIGH");
    }
}
