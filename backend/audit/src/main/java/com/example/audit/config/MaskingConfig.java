package com.example.audit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.audit.infra.masking.JpaUnmaskAuditSink;
import com.example.common.masking.MaskingService;
import com.example.common.masking.PolicyMaskingStrategy;
import com.example.common.masking.MaskingStrategy;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;

@Configuration
public class MaskingConfig {

    @Bean
    public MaskingService maskingService(MaskingStrategy maskingStrategy, JpaUnmaskAuditSink sink) {
        return new MaskingService(maskingStrategy, sink);
    }

    @Bean
    public MaskingStrategy maskingStrategy(PolicySettingsProvider policySettingsProvider) {
        PolicyToggleSettings settings = policySettingsProvider.currentSettings();
        return new PolicyMaskingStrategy(settings);
    }
}
