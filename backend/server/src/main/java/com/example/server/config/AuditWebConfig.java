package com.example.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.audit.AuditPort;
import com.example.server.audit.RequestAuditInterceptor;
import com.example.server.audit.SensitiveApiFilter;

@Configuration
public class AuditWebConfig implements WebMvcConfigurer {

    private final AuditPort auditPort;

    public AuditWebConfig(AuditPort auditPort) {
        this.auditPort = auditPort;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestAuditInterceptor(auditPort))
                .addPathPatterns("/api/**");
    }

    @Bean
    public FilterRegistrationBean<SensitiveApiFilter> sensitiveApiFilter(@Value("${audit.sensitive-api.reason-parameter:reasonCode}") String reasonParam,
                                                                         @Value("${audit.sensitive-api.legal-basis-parameter:legalBasisCode}") String legalParam) {
        FilterRegistrationBean<SensitiveApiFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new SensitiveApiFilter(auditPort, reasonParam, legalParam));
        bean.addUrlPatterns("/api/*");
        bean.setOrder(0);
        return bean;
    }
}
