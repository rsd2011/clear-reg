package com.example.audit.infra.siem;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.example.audit.AuditEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class HttpSiemForwarderFilterTest {

    @Test
    @DisplayName("whitelist가 지정되면 해당 필드만 전송한다")
    void whitelistFiltersPayload() throws Exception {
        SiemProperties props = new SiemProperties();
        props.setEnabled(true);
        props.setEndpoint("http://localhost/siem");
        props.setWhitelist(java.util.List.of("eventType"));

        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        HttpSiemForwarder forwarder = new HttpSiemForwarder(props, mapper);
        CapturingRestTemplate rest = new CapturingRestTemplate();
        forwarder.setRestTemplate(rest);

        AuditEvent event = AuditEvent.builder().eventType("FILTER_TEST").eventTime(Instant.now())
                .actor(com.example.audit.Actor.builder().id("a1").build()).build();
        forwarder.forward(event);

        Map<String, Object> body = mapper.readValue(rest.captured.getBody().toString(), Map.class);
        assertThat(body).containsOnlyKeys("eventType");
    }

    private static class CapturingRestTemplate extends RestTemplate {
        HttpEntity<?> captured;

        @Override
        public <T> ResponseEntity<T> postForEntity(String url, Object request, Class<T> responseType, Object... uriVariables) {
            captured = (HttpEntity<?>) request;
            return ResponseEntity.ok().build();
        }
    }
}
