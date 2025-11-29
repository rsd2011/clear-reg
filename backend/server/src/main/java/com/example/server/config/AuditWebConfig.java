package com.example.server.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.audit.AuditPort;
import com.example.audit.NoopAuditPort;
import com.example.common.policy.RowAccessPolicyProvider;
import com.example.server.audit.PolicyMaskingFilter;
import com.example.server.audit.SensitiveApiFilter;
import com.example.server.config.SensitiveApiProperties;

@Configuration
@EnableConfigurationProperties(SensitiveApiProperties.class)
public class AuditWebConfig {

    private final ObjectProvider<AuditPort> auditPortProvider;
    private final SensitiveApiProperties sensitiveApiProperties;
    private final ObjectProvider<RowAccessPolicyProvider> rowAccessPolicyProvider;

    public AuditWebConfig(ObjectProvider<AuditPort> auditPortProvider,
                         SensitiveApiProperties sensitiveApiProperties,
                         ObjectProvider<RowAccessPolicyProvider> rowAccessPolicyProvider) {
        this.auditPortProvider = auditPortProvider;
        this.sensitiveApiProperties = sensitiveApiProperties;
        this.rowAccessPolicyProvider = rowAccessPolicyProvider;
    }

    @Bean
    public FilterRegistrationBean<SensitiveApiFilter> sensitiveApiFilter() {
        FilterRegistrationBean<SensitiveApiFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new SensitiveApiFilter(auditPortProvider.getIfAvailable(NoopAuditPort::new), sensitiveApiProperties));
        bean.addUrlPatterns("/api/*");
        bean.setOrder(0);
        return bean;
    }

    @Bean
    public FilterRegistrationBean<PolicyMaskingFilter> policyMaskingFilter() {
        FilterRegistrationBean<PolicyMaskingFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new PolicyMaskingFilter(rowAccessPolicyProvider.getIfAvailable(() -> query -> java.util.Optional.empty())));
        bean.addUrlPatterns("/api/*");
        bean.setOrder(-1); // sensitive 필터보다 먼저 적용
        return bean;
    }
}
