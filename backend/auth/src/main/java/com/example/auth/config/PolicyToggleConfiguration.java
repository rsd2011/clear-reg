package com.example.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import com.example.auth.security.PolicyToggleProvider;

@Configuration
@EnableConfigurationProperties(PolicyToggleProperties.class)
public class PolicyToggleConfiguration {

    @Bean
    @ConditionalOnMissingBean(PolicyToggleProvider.class)
    public PolicyToggleProvider policyToggleProvider(PolicyToggleProperties properties) {
        return new PropertiesPolicyToggleProvider(properties);
    }

    private static final class PropertiesPolicyToggleProvider implements PolicyToggleProvider {

        private final PolicyToggleProperties properties;

        private PropertiesPolicyToggleProvider(PolicyToggleProperties properties) {
            this.properties = properties;
        }

        @Override
        public boolean isPasswordPolicyEnabled() {
            return properties.isPasswordPolicyEnabled();
        }

        @Override
        public boolean isPasswordHistoryEnabled() {
            return properties.isPasswordHistoryEnabled();
        }

        @Override
        public boolean isAccountLockEnabled() {
            return properties.isAccountLockEnabled();
        }

        @Override
        public java.util.List<com.example.auth.LoginType> enabledLoginTypes() {
            return java.util.List.copyOf(properties.getEnabledLoginTypes());
        }
    }
}
