package com.example.audit.infra.siem;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.example.audit.AuditEvent;
import com.example.audit.Actor;
import com.example.audit.ActorType;
import com.example.audit.RiskLevel;
import com.fasterxml.jackson.databind.ObjectMapper;

class HttpSiemForwarderTest {

    @Test
    @DisplayName("SIEM endpoint로 HTTP POST를 전송한다")
    void forwardsToSiem() {
        SiemProperties props = new SiemProperties();
        props.setEnabled(true);
        props.setEndpoint("https://siem.example.com/audit");
        props.setApiKey("token");
        HttpSiemForwarder forwarder = new HttpSiemForwarder(props, new ObjectMapper().findAndRegisterModules());
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        forwarder.setRestTemplate(restTemplate);
        var entityCaptor = org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        Mockito.when(restTemplate.postForEntity(Mockito.anyString(), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        AuditEvent event = AuditEvent.builder()
                .eventId(UUID.randomUUID())
                .eventTime(Instant.now())
                .eventType("TEST")
                .action("FORWARD")
                .actor(Actor.builder().id("u1").type(ActorType.HUMAN).build())
                .riskLevel(RiskLevel.LOW)
                .build();

        forwarder.forward(event);

        verify(restTemplate).postForEntity(Mockito.anyString(), entityCaptor.capture(), eq(Void.class));
        HttpHeaders headers = entityCaptor.getValue().getHeaders();
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token");
    }

    @Test
    @DisplayName("전송 실패 시 예외를 삼키고 로그만 남긴다")
    void failureIsSwallowed() {
        SiemProperties props = new SiemProperties();
        props.setEnabled(true);
        props.setEndpoint("https://siem.example.com/audit");
        HttpSiemForwarder forwarder = new HttpSiemForwarder(props, new ObjectMapper().findAndRegisterModules());
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        forwarder.setRestTemplate(restTemplate);
        Mockito.doThrow(new RuntimeException("fail"))
                .when(restTemplate).postForEntity(Mockito.anyString(), any(HttpEntity.class), eq(Void.class));

        AuditEvent event = AuditEvent.builder()
                .eventId(UUID.randomUUID())
                .eventTime(Instant.now())
                .eventType("TEST")
                .action("FORWARD")
                .build();

        var entityCaptor = org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        assertThatCode(() -> forwarder.forward(event)).doesNotThrowAnyException();
        verify(restTemplate).postForEntity(Mockito.anyString(), entityCaptor.capture(), eq(Void.class));
        HttpHeaders headers = entityCaptor.getValue().getHeaders();
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isNull();
    }
}
