package com.example.batch.worker.config;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;

@Configuration
public class DwWorkerPolicyConfig {

    @Bean
    @ConditionalOnMissingBean(PolicySettingsProvider.class)
    public PolicySettingsProvider policySettingsProvider() {
        PolicyToggleSettings defaults = new PolicyToggleSettings(
                false,
                false,
                false,
                List.of("PASSWORD"),
                20 * 1024 * 1024L,
                List.of("pdf", "csv", "txt"),
                true,
                365
        );
        return () -> defaults;
    }
}
