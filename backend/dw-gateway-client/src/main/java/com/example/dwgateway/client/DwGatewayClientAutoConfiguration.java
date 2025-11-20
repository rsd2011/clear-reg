package com.example.dwgateway.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.example.dwgateway.dw.DwBatchPort;
import com.example.dwgateway.dw.DwIngestionPolicyPort;
import com.example.dwgateway.dw.DwOrganizationPort;
import com.example.file.port.FileManagementPort;

import org.springframework.retry.support.RetryTemplate;

@Configuration
@ConditionalOnClass(RestTemplate.class)
@EnableConfigurationProperties(DwGatewayClientProperties.class)
@ConditionalOnProperty(prefix = "dw.gateway.client", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DwGatewayClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "dwGatewayRestTemplate")
    RestTemplate dwGatewayRestTemplate(DwGatewayClientProperties properties, RestTemplateBuilder builder) {
        RestTemplateBuilder effectiveBuilder = builder
                .rootUri(properties.getBaseUri().toString())
                .setConnectTimeout(properties.getConnectTimeout())
                .setReadTimeout(properties.getReadTimeout());
        if (StringUtils.hasText(properties.getServiceToken())) {
            effectiveBuilder = effectiveBuilder.additionalInterceptors((request, body, execution) -> {
                request.getHeaders().set(properties.getServiceTokenHeader(), properties.getServiceToken());
                return execution.execute(request, body);
            });
        }
        return effectiveBuilder.build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "dwGatewayRetryTemplate")
    RetryTemplate dwGatewayRetryTemplate(DwGatewayClientProperties properties) {
        DwGatewayClientProperties.Retry retry = properties.getRetry();
        return RetryTemplate.builder()
                .maxAttempts(retry.getMaxAttempts())
                .fixedBackoff(retry.getBackoff())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    DwIngestionPolicyPort dwIngestionPolicyPortClient(RestTemplate dwGatewayRestTemplate,
                                                      RetryTemplate dwGatewayRetryTemplate) {
        return new DwIngestionPolicyPortClient(dwGatewayRestTemplate, dwGatewayRetryTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    DwBatchPort dwBatchPortClient(RestTemplate dwGatewayRestTemplate, RetryTemplate dwGatewayRetryTemplate) {
        return new DwBatchPortClient(dwGatewayRestTemplate, dwGatewayRetryTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    DwOrganizationPort dwOrganizationPortClient(RestTemplate dwGatewayRestTemplate,
                                                RetryTemplate dwGatewayRetryTemplate) {
        return new DwOrganizationPortClient(dwGatewayRestTemplate, dwGatewayRetryTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    FileManagementPort fileManagementPortClient(RestTemplate dwGatewayRestTemplate,
                                               RetryTemplate dwGatewayRetryTemplate) {
        return new FileManagementPortClient(dwGatewayRestTemplate, dwGatewayRetryTemplate);
    }
}
