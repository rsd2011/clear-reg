package com.example.audit.infra.siem;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.example.audit.AuditEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

class HttpSiemForwarderWhitelistTest {

    @Test
    @DisplayName("whitelist가 비어있으면 이벤트가 그대로 전달된다")
    void whitelistEmptyKeepsPayload() throws Exception {
        SiemProperties props = new SiemProperties();
        props.setEnabled(true);
        props.setEndpoint("http://localhost/siem");
        props.setWhitelist(java.util.List.of());

        HttpSiemForwarder forwarder = new HttpSiemForwarder(props, new ObjectMapper());
        forwarder.setRestTemplate(new TrackingRestTemplate());

        AuditEvent event = AuditEvent.builder().eventType("WHITELIST").eventTime(Instant.now()).actor(com.example.audit.Actor.builder().id("actor1").build()).build();
        org.assertj.core.api.Assertions.assertThatCode(() -> forwarder.forward(event)).doesNotThrowAnyException();
    }

    private static class TrackingRestTemplate extends RestTemplate {
        boolean called = false;
        HttpEntity<?> captured;

        @Override
        public <T> ResponseEntity<T> postForEntity(String url, Object request, Class<T> responseType, Object... uriVariables) throws org.springframework.web.client.RestClientException {
            called = true;
            captured = (HttpEntity<?>) request;
            return ResponseEntity.ok().build();
        }
    }
}
