package com.example.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ErrorResponseTest {

    @Test
    void storesFieldsAndDefaultsTraceNull() {
        ErrorResponse res = new ErrorResponse("CODE", "msg");
        assertThat(res.code()).isEqualTo("CODE");
        assertThat(res.message()).isEqualTo("msg");
        assertThat(res.traceId()).isNull();
    }
}
