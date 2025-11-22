package com.example.dwgateway.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DwGatewayClientPropertiesTest {

    @Test
    @DisplayName("기본값은 enabled=true, baseUri=http://localhost:8081, retry 3회/200ms")
    void defaults() {
        DwGatewayClientProperties props = new DwGatewayClientProperties();

        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getBaseUri()).isEqualTo(URI.create("http://localhost:8081"));
        assertThat(props.getRetry().getMaxAttempts()).isEqualTo(3);
        assertThat(props.getRetry().getBackoff()).isEqualTo(Duration.ofMillis(200));
    }

    @Test
    @DisplayName("Retry 설정을 세터로 변경할 수 있다")
    void retryCanBeConfigured() {
        DwGatewayClientProperties props = new DwGatewayClientProperties();
        props.getRetry().setMaxAttempts(5);
        props.getRetry().setBackoff(Duration.ofSeconds(1));

        assertThat(props.getRetry().getMaxAttempts()).isEqualTo(5);
        assertThat(props.getRetry().getBackoff()).isEqualTo(Duration.ofSeconds(1));
    }

    @Test
    @DisplayName("enabled 플래그를 비활성화할 수 있다")
    void canDisableClient() {
        DwGatewayClientProperties props = new DwGatewayClientProperties();
        props.setEnabled(false);

        assertThat(props.isEnabled()).isFalse();
    }
}
