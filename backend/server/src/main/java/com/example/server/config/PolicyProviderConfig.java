package com.example.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.stream.Collectors;

import com.example.auth.LoginType;
import com.example.auth.config.PolicyToggleProperties;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;
import com.example.policy.PolicyAdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@Configuration
public class PolicyProviderConfig {

    @Bean
    public PolicySettingsProvider policySettingsProvider(PolicyAdminService policyAdminService) {
        return new PolicyAdminService.DatabasePolicySettingsProvider(policyAdminService);
    }

    @Bean(name = "yamlObjectMapper")
    public ObjectMapper yamlObjectMapper() {
        return new ObjectMapper(new YAMLFactory());
    }

    @Bean
    public PolicyToggleSettings defaultPolicyToggleSettings(PolicyToggleProperties properties) {
        var enabled = properties.getEnabledLoginTypes().stream()
                .map(LoginType::name)
                .collect(Collectors.toList());
        return new PolicyToggleSettings(properties.isPasswordPolicyEnabled(),
                properties.isPasswordHistoryEnabled(),
                properties.isAccountLockEnabled(),
                enabled);
    }
}
