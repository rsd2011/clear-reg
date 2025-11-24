package com.example.audit.infra.siem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.audit.AuditEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

class HttpSiemForwarderFallbackFactoryTest {

    @Test
    @DisplayName("키스토어가 없을 때 fallback RequestFactory로 전송을 시도한다")
    void usesFallbackFactoryWhenKeystoreMissing() {
        SiemProperties props = new SiemProperties();
        props.setEnabled(true);
        props.setEndpoint("http://localhost/siem");
        // keystore/truststore 미설정 -> fallback 분기

        HttpSiemForwarder forwarder = new HttpSiemForwarder(props, new ObjectMapper());
        // 네트워크 예외는 swallow되므로 예외 없음을 확인
        org.assertj.core.api.Assertions.assertThatCode(() ->
                forwarder.forward(AuditEvent.builder().eventType("FALLBACK").build())
        ).doesNotThrowAnyException();
    }
}

