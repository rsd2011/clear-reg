package com.example.dwgateway.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DwGatewayClientExceptionTest {

    @Test
    @DisplayName("메시지와 cause를 보존한다")
    void storesMessageAndCause() {
        RuntimeException cause = new RuntimeException("root");
        DwGatewayClientException ex = new DwGatewayClientException("failed", cause);

        assertThat(ex).hasMessageContaining("failed");
        assertThat(ex).hasCause(cause);
    }
}
