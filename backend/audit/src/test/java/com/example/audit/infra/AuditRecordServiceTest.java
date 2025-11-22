package com.example.audit.infra;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;

import com.example.audit.Actor;
import com.example.audit.ActorType;
import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPolicySnapshot;
import com.example.audit.RiskLevel;
import com.example.audit.infra.persistence.AuditLogRepository;
import com.example.audit.infra.policy.AuditPolicyResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.audit.infra.persistence.AuditLogEntity;

class AuditRecordServiceTest {

    AuditLogRepository repository = Mockito.mock(AuditLogRepository.class);
    AuditPolicyResolver policyResolver = Mockito.mock(AuditPolicyResolver.class);
    KafkaTemplate<String, String> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void record_persistsAndPublishes_whenEnabled() {
        given(policyResolver.resolve(any(), any()))
                .willReturn(Optional.of(AuditPolicySnapshot.builder().enabled(true).build()));
        AuditRecordService service = new AuditRecordService(repository, policyResolver, objectMapper, kafkaTemplate, "audit.events.v1", false, "", "default");

        service.record(sampleEvent(), AuditMode.STRICT);

        verify(repository).save(any());
        verify(kafkaTemplate).send(any(), any(), any());
    }

    @Test
    void record_skipsWhenPolicyDisabled() {
        given(policyResolver.resolve(any(), any()))
                .willReturn(Optional.of(AuditPolicySnapshot.builder().enabled(false).build()));
        AuditRecordService service = new AuditRecordService(repository, policyResolver, objectMapper, kafkaTemplate, "audit.events.v1", false, "", "default");

        service.record(sampleEvent(), AuditMode.STRICT);

        Mockito.verifyNoInteractions(repository, kafkaTemplate);
    }

    @Test
    void record_strictModeThrowsOnPersistFailure() {
        given(policyResolver.resolve(any(), any()))
                .willReturn(Optional.of(AuditPolicySnapshot.builder().enabled(true).build()));
        given(repository.save(any())).willThrow(new IllegalStateException("fail"));
        AuditRecordService service = new AuditRecordService(repository, policyResolver, objectMapper, kafkaTemplate, "audit.events.v1", false, "", "default");

        assertThatThrownBy(() -> service.record(sampleEvent(), AuditMode.STRICT))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void record_asyncFallbackDoesNotThrowOnPersistFailure() {
        given(policyResolver.resolve(any(), any()))
                .willReturn(Optional.of(AuditPolicySnapshot.builder().enabled(true).build()));
        given(repository.save(any())).willThrow(new IllegalStateException("fail"));
        AuditRecordService service = new AuditRecordService(repository, policyResolver, objectMapper, null, "audit.events.v1", false, "", "default");

        service.record(sampleEvent(), AuditMode.ASYNC_FALLBACK);
    }

    private AuditEvent sampleEvent() {
        return AuditEvent.builder()
                .eventType("LOGIN")
                .moduleName("auth")
                .action("LOGIN_SUCCESS")
                .actor(Actor.builder().id("user1").type(ActorType.HUMAN).build())
                .riskLevel(RiskLevel.LOW)
                .success(true)
                .build();
    }

    @Test
    void record_setsHashChainUsingPreviousHash() {
        given(policyResolver.resolve(any(), any()))
                .willReturn(Optional.of(AuditPolicySnapshot.builder().enabled(true).build()));
        AuditLogEntity prev = new AuditLogEntity(java.util.UUID.randomUUID(), java.time.Instant.now(), "TYPE", "mod", "act",
                "actor", "HUMAN", "role", "dept", "SUBJECT", "123",
                "INTERNAL", "127.0.0.1", "JUnit", "dev-1",
                true, "OK", "R", "T", "PIPA", "LOW", "before", "after", "{}", "prevhash");
        when(repository.findTopByOrderByEventTimeDesc()).thenReturn(Optional.of(prev));
        AuditRecordService service = new AuditRecordService(repository, policyResolver, objectMapper, null, "audit.events.v1", false, "", "default");

        service.record(sampleEvent(), AuditMode.ASYNC_FALLBACK);

        verify(repository).save(Mockito.argThat(entity -> entity.getHashChain() != null && !entity.getHashChain().isEmpty()));
    }
}
