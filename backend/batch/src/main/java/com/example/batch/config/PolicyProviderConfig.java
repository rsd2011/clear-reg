package com.example.batch.config;

import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import com.example.auth.LoginType;
import com.example.auth.config.PolicyToggleProperties;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@Configuration
public class PolicyProviderConfig {

    /**
     * Fallback no-op PolicySettingsProvider.
     * <p>
     * 다른 provider가 활성화되지 않을 경우에만 등록됩니다.
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(PolicySettingsProvider.class)
    public PolicySettingsProvider noopPolicySettingsProvider() {
        return () -> null;
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
                enabled,
                properties.getMaxFileSizeBytes(),
                properties.getAllowedFileExtensions().stream().map(String::toLowerCase).toList(),
                properties.isStrictMimeValidation(),
                properties.getFileRetentionDays(),
                properties.isAuditEnabled(),
                properties.isAuditReasonRequired(),
                properties.isAuditSensitiveApiDefaultOn(),
                properties.getAuditRetentionDays(),
                properties.isAuditStrictMode(),
                properties.getAuditRiskLevel(),
                properties.isAuditMaskingEnabled(),
                properties.getAuditSensitiveEndpoints(),
                properties.getAuditUnmaskRoles(),
                properties.isAuditPartitionEnabled(),
                properties.getAuditPartitionCron(),
                properties.getAuditPartitionPreloadMonths(),
                properties.isAuditMonthlyReportEnabled(),
                properties.getAuditMonthlyReportCron(),
                properties.isAuditLogRetentionEnabled(),
                properties.getAuditLogRetentionCron(),
                properties.isAuditColdArchiveEnabled(),
                properties.getAuditColdArchiveCron(),
                properties.isAuditRetentionCleanupEnabled(),
                properties.getAuditRetentionCleanupCron(),
                java.util.Map.of());
    }
}
