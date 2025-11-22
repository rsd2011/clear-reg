package com.example.dwgateway.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DwGatewayClientPropertiesHeaderTest {

    @Test
    @DisplayName("serviceToken과 헤더 이름을 설정하면 그대로 반영된다")
    void serviceTokenAndHeaderAreApplied() {
        DwGatewayClientProperties props = new DwGatewayClientProperties();
        props.setServiceToken("token-123");
        props.setServiceTokenHeader("X-Custom-Token");
        props.setBaseUri(URI.create("https://api.example.com"));
        props.setConnectTimeout(Duration.ofSeconds(5));
        props.setReadTimeout(Duration.ofSeconds(7));

        assertThat(props.getServiceToken()).isEqualTo("token-123");
        assertThat(props.getServiceTokenHeader()).isEqualTo("X-Custom-Token");
        assertThat(props.getBaseUri()).isEqualTo(URI.create("https://api.example.com"));
        assertThat(props.getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(props.getReadTimeout()).isEqualTo(Duration.ofSeconds(7));
    }
}
