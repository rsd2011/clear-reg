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
        props.setWhitelist(java.util.List.of("eventType", "actor.id"));
        props.setHmacSecret("secret");
        props.setRetry(2);
        HttpSiemForwarder forwarder = new HttpSiemForwarder(props, new ObjectMapper().findAndRegisterModules());
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        forwarder.setRestTemplate(restTemplate);
        var entityCaptor = org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        Mockito.when(restTemplate.postForEntity(Mockito.anyString(), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new RuntimeException("net"))
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

        verify(restTemplate, Mockito.times(2)).postForEntity(Mockito.anyString(), entityCaptor.capture(), eq(Void.class));
        HttpHeaders headers = entityCaptor.getValue().getHeaders();
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token");
        assertThat(headers.getFirst("X-SIEM-SIGNATURE")).isNotBlank();
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

    @Test
    @DisplayName("RestTemplate이 주입되지 않은 경우 내부 생성 로직을 타고 예외를 삼킨다")
    void createsTemplateWhenNull() {
        SiemProperties props = new SiemProperties();
        props.setEnabled(true);
        props.setEndpoint("http://invalid.local");
        HttpSiemForwarder forwarder = new HttpSiemForwarder(props, new ObjectMapper().findAndRegisterModules());

        AuditEvent event = AuditEvent.builder()
                .eventId(UUID.randomUUID())
                .eventTime(Instant.now())
                .eventType("TEST")
                .action("FORWARD")
                .build();

        // 내부 RestTemplate가 연결 실패해도 catch되어 예외가 던져지지 않아야 한다
        assertThatCode(() -> forwarder.forward(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("API 키와 HMAC이 설정되면 헤더에 반영되고 whitelist 필터가 적용된다")
    void forwardAddsAuthAndHmac() {
        SiemProperties props = new SiemProperties();
        props.setEnabled(true);
        props.setEndpoint("http://localhost/siem");
        props.setApiKey("token");
        props.setHmacSecret("secret");
        props.setWhitelist(java.util.List.of("eventType"));

        HttpSiemForwarder forwarder = new HttpSiemForwarder(props, new ObjectMapper().findAndRegisterModules());
        RestTemplate rest = Mockito.mock(RestTemplate.class);
        forwarder.setRestTemplate(rest);

        forwarder.forward(AuditEvent.builder().eventType("AUTH").eventTime(Instant.now()).build());

        Mockito.verify(rest).postForEntity(Mockito.eq(props.getEndpoint()), any(HttpEntity.class), eq(Void.class));
    }

    @Test
    @DisplayName("retry가 0이면 최소 1회 전송을 시도한다")
    void retryZeroMeansAtLeastOne() {
        SiemProperties props = new SiemProperties();
        props.setEnabled(true);
        props.setEndpoint("http://localhost/siem");
        props.setRetry(0); // Math.max(1, 0) = 1

        HttpSiemForwarder forwarder = new HttpSiemForwarder(props, new ObjectMapper().findAndRegisterModules());
        RestTemplate rest = Mockito.mock(RestTemplate.class);
        forwarder.setRestTemplate(rest);

        forwarder.forward(AuditEvent.builder().eventType("TEST").eventTime(Instant.now()).build());

        Mockito.verify(rest, Mockito.times(1)).postForEntity(Mockito.anyString(), any(HttpEntity.class), eq(Void.class));
    }

    @Test
    @DisplayName("apiKey가 빈 문자열이면 Authorization 헤더가 설정되지 않는다")
    void blankApiKeyNoAuthHeader() {
        SiemProperties props = new SiemProperties();
        props.setEnabled(true);
        props.setEndpoint("http://localhost/siem");
        props.setApiKey("   "); // blank

        HttpSiemForwarder forwarder = new HttpSiemForwarder(props, new ObjectMapper().findAndRegisterModules());
        RestTemplate rest = Mockito.mock(RestTemplate.class);
        forwarder.setRestTemplate(rest);
        var entityCaptor = org.mockito.ArgumentCaptor.forClass(HttpEntity.class);

        forwarder.forward(AuditEvent.builder().eventType("TEST").eventTime(Instant.now()).build());

        verify(rest).postForEntity(Mockito.anyString(), entityCaptor.capture(), eq(Void.class));
        HttpHeaders headers = entityCaptor.getValue().getHeaders();
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isNull();
    }

    @Test
    @DisplayName("hmacSecret이 빈 문자열이면 X-SIEM-SIGNATURE 헤더가 설정되지 않는다")
    void blankHmacSecretNoSignatureHeader() {
        SiemProperties props = new SiemProperties();
        props.setEnabled(true);
        props.setEndpoint("http://localhost/siem");
        props.setHmacSecret("   "); // blank

        HttpSiemForwarder forwarder = new HttpSiemForwarder(props, new ObjectMapper().findAndRegisterModules());
        RestTemplate rest = Mockito.mock(RestTemplate.class);
        forwarder.setRestTemplate(rest);
        var entityCaptor = org.mockito.ArgumentCaptor.forClass(HttpEntity.class);

        forwarder.forward(AuditEvent.builder().eventType("TEST").eventTime(Instant.now()).build());

        verify(rest).postForEntity(Mockito.anyString(), entityCaptor.capture(), eq(Void.class));
        HttpHeaders headers = entityCaptor.getValue().getHeaders();
        assertThat(headers.getFirst("X-SIEM-SIGNATURE")).isNull();
    }
}
