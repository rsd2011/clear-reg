package com.example.audit.infra.siem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.audit.AuditEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

class HttpSiemForwarderDisabledTest {

    @Test
    @DisplayName("enabled=false이면 전송을 건너뛴다")
    void skipsWhenDisabled() {
        SiemProperties props = new SiemProperties();
        props.setEnabled(false);
        props.setEndpoint("http://localhost/siem");

        HttpSiemForwarder forwarder = new HttpSiemForwarder(props, new ObjectMapper());

        org.assertj.core.api.Assertions.assertThatCode(() ->
                forwarder.forward(AuditEvent.builder().eventType("DISABLED").build())
        ).doesNotThrowAnyException();
    }
}

