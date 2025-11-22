package com.example.dwgateway.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestTemplate;

import com.example.dwgateway.dw.DwBatchPort;
import com.example.dwgateway.dw.DwIngestionPolicyPort;
import com.example.dwgateway.dw.DwOrganizationPort;
import com.example.file.port.FileManagementPort;

class DwGatewayClientAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration.class,
                    DwGatewayClientAutoConfiguration.class));

    @Test
    @DisplayName("AutoConfiguration이 기본 RestTemplate/RetryTemplate과 포트 클라이언트를 등록한다")
    void createsBeansWhenEnabled() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(RestTemplate.class);
            assertThat(context).hasSingleBean(org.springframework.retry.support.RetryTemplate.class);
            assertThat(context).hasSingleBean(DwBatchPort.class);
            assertThat(context).hasSingleBean(DwIngestionPolicyPort.class);
            assertThat(context).hasSingleBean(DwOrganizationPort.class);
            assertThat(context).hasSingleBean(FileManagementPort.class);
        });
    }

    @Test
    @DisplayName("enabled=false이면 빈을 생성하지 않는다")
    void disablesWhenPropertyFalse() {
        contextRunner.withPropertyValues("dw.gateway.client.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(RestTemplate.class);
        });
    }
}
