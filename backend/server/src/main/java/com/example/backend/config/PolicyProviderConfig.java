package com.example.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.auth.security.PolicyToggleProvider;
import com.example.backend.policy.PolicyAdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@Configuration
public class PolicyProviderConfig {

    @Bean
    public PolicyToggleProvider policyToggleProvider(PolicyAdminService policyAdminService) {
        return new PolicyAdminService.DatabasePolicyToggleProvider(policyAdminService);
    }

    @Bean(name = "yamlObjectMapper")
    public ObjectMapper yamlObjectMapper() {
        return new ObjectMapper(new YAMLFactory());
    }
}
