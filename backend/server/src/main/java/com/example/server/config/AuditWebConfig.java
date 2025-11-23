package com.example.server.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.audit.AuditPort;
import com.example.server.audit.RequestAuditInterceptor;
import com.example.server.audit.SensitiveApiFilter;
import com.example.server.audit.DataPolicyMaskingFilter;
import com.example.server.config.SensitiveApiProperties;
import com.example.audit.NoopAuditPort;
import com.example.common.policy.DataPolicyProvider;

@Configuration
@EnableConfigurationProperties(SensitiveApiProperties.class)
public class AuditWebConfig implements WebMvcConfigurer {

    private final ObjectProvider<AuditPort> auditPortProvider;
    private final SensitiveApiProperties sensitiveApiProperties;
    private final ObjectProvider<DataPolicyProvider> dataPolicyProvider;

    public AuditWebConfig(ObjectProvider<AuditPort> auditPortProvider,
                         SensitiveApiProperties sensitiveApiProperties,
                         ObjectProvider<DataPolicyProvider> dataPolicyProvider) {
        this.auditPortProvider = auditPortProvider;
        this.sensitiveApiProperties = sensitiveApiProperties;
        this.dataPolicyProvider = dataPolicyProvider;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestAuditInterceptor(auditPortProvider.getIfAvailable(NoopAuditPort::new)))
                .addPathPatterns("/api/**");
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
    public FilterRegistrationBean<DataPolicyMaskingFilter> dataPolicyMaskingFilter() {
        FilterRegistrationBean<DataPolicyMaskingFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new DataPolicyMaskingFilter(dataPolicyProvider.getIfAvailable(() -> query -> java.util.Optional.empty())));
        bean.addUrlPatterns("/api/*");
        bean.setOrder(-1); // sensitive 필터보다 먼저 적용
        return bean;
    }
}
