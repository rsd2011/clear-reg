package com.example.audit.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class AuditKafkaTemplateTest {

    @Test
    void record_invokesKafkaTemplateWhenEnabled() {
        var repository = Mockito.mock(AuditLogRepository.class);
        var resolver = Mockito.mock(AuditPolicyResolver.class);
        Mockito.when(resolver.resolve(any(), any())).thenReturn(java.util.Optional.of(AuditPolicySnapshot.builder().enabled(true).build()));
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> template = Mockito.mock(KafkaTemplate.class);

        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        AuditRecordService service = new AuditRecordService(repository, resolver, mapper, template, "audit.events.v1", false, "", "default", new com.example.audit.infra.masking.MaskingProperties(), null);

        AuditEvent event = AuditEvent.builder()
                .eventId(UUID.randomUUID())
                .eventTime(Instant.now())
                .eventType("TEST")
                .moduleName("audit")
                .action("SMOKE")
                .actor(Actor.builder().id("tester").type(ActorType.HUMAN).build())
                .riskLevel(RiskLevel.LOW)
                .build();

        service.record(event, AuditMode.ASYNC_FALLBACK);

        verify(template).send(Mockito.eq("audit.events.v1"), Mockito.eq(event.getEventId().toString()), Mockito.anyString());
        assertThat(event.getEventId()).isNotNull();
    }
}
