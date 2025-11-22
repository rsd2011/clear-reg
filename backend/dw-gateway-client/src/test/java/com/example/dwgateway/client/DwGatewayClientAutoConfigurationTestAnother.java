package com.example.dwgateway.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;

class DwGatewayClientAutoConfigurationTestAnother {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DwGatewayClientAutoConfiguration.class))
            .withPropertyValues("dw.gateway.client.enabled=false");

    @Test
    @DisplayName("enabled=false면 RestTemplate 빈을 생성하지 않는다")
    void disabledSkipsRestTemplate() {
        runner.run(ctx -> {
            assertThat(ctx.containsBean("dwGatewayRestTemplate")).isFalse();
        });
    }

    @Test
    @DisplayName("serviceToken이 설정되면 인터셉터가 추가된다")
    void addsServiceTokenInterceptor() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RestTemplateAutoConfiguration.class, DwGatewayClientAutoConfiguration.class))
                .withPropertyValues(
                        "dw.gateway.client.enabled=true",
                        "dw.gateway.client.service-token=abc123",
                        "dw.gateway.client.service-token-header=X-Service-Token")
                .run(ctx -> {
                    assertThat(ctx.containsBean("dwGatewayRestTemplate")).isTrue();
                });
    }
}
