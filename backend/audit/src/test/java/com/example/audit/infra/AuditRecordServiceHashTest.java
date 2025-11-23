package com.example.audit.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;

import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPolicySnapshot;
import com.example.audit.infra.masking.MaskingProperties;
import com.example.audit.infra.persistence.AuditLogEntity;
import com.example.audit.infra.persistence.AuditLogRepository;
import com.example.audit.infra.policy.AuditPolicyResolver;
import com.example.common.masking.MaskingService;
import com.example.common.masking.MaskingTarget;
import com.example.common.masking.SubjectType;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("AuditRecordService 해시체인/HMAC/발행 실패 분기")
class AuditRecordServiceHashTest {

    AuditLogRepository repository = Mockito.mock(AuditLogRepository.class);
    AuditPolicyResolver resolver = Mockito.mock(AuditPolicyResolver.class);
    KafkaTemplate<String, String> kafka = Mockito.mock(KafkaTemplate.class);
    MaskingProperties maskingProperties = new MaskingProperties();
    MaskingService maskingService = Mockito.mock(MaskingService.class);
    ObjectMapper mapper = new ObjectMapper();

    @Test
    void hmacEnabledComputesHash() {
        given(resolver.resolve(any(), any())).willReturn(Optional.of(AuditPolicySnapshot.builder().enabled(true).build()));
        given(repository.findTopByOrderByEventTimeDesc()).willReturn(Optional.empty());

        AuditRecordService service = new AuditRecordService(repository, resolver, mapper, null,
                "audit.events.v1", "", null,
                true, "secret", "kid1", maskingProperties, maskingService);

        AuditEvent event = AuditEvent.builder().eventId(UUID.randomUUID()).eventTime(Instant.now()).eventType("TYPE").action("ACT").build();

        service.record(event, AuditMode.ASYNC_FALLBACK, MaskingTarget.builder().subjectType(SubjectType.CUSTOMER_INDIVIDUAL).build());

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getHashChain()).isNotBlank();
    }

    @Test
    void publishFailureInStrictThrows() {
        given(resolver.resolve(any(), any())).willReturn(Optional.of(AuditPolicySnapshot.builder().enabled(true).build()));
        KafkaTemplate<String, String> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        Mockito.doThrow(new RuntimeException("send fail")).when(kafkaTemplate).send(any(), any(), any());

        AuditRecordService service = new AuditRecordService(repository, resolver, mapper, kafkaTemplate,
                "topic", "", kafkaTemplate,
                false, "", "kid", maskingProperties, maskingService);

        AuditEvent event = AuditEvent.builder().eventId(UUID.randomUUID()).eventTime(Instant.now()).eventType("TYPE").action("ACT").build();

        assertThatThrownBy(() -> service.record(event, AuditMode.STRICT)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void maskingServiceRenderUsedWhenProvided() {
        given(resolver.resolve(any(), any())).willReturn(Optional.of(AuditPolicySnapshot.builder().enabled(true).build()));
        given(repository.findTopByOrderByEventTimeDesc()).willReturn(Optional.empty());
        given(maskingService.render(any(), any(), any())).willReturn("RAW");

        AuditRecordService service = new AuditRecordService(repository, resolver, mapper, null,
                "audit.events.v1", "", null,
                false, "", "kid1", maskingProperties, maskingService);

        AuditEvent event = AuditEvent.builder().eventId(UUID.randomUUID()).eventTime(Instant.now()).eventType("TYPE").action("ACT")
                .beforeSummary("4111-1111-1111-1111").build();
        service.record(event, AuditMode.ASYNC_FALLBACK, MaskingTarget.builder().subjectType(SubjectType.CUSTOMER_INDIVIDUAL).maskRule("FULL").build());

        verify(maskingService).render(any(), any(), Mockito.eq("beforeSummary"));
    }

    @Test
    void inlineMaskableMethodsExecuted() {
        given(resolver.resolve(any(), any())).willReturn(Optional.of(AuditPolicySnapshot.builder().enabled(true).build()));
        MaskingService realMaskingService = new MaskingService(target -> true); // always mask -> uses maskable.masked()

        AuditRecordService service = new AuditRecordService(repository, resolver, mapper, null, "audit.events.v1", "", null, false, "", "kid1", maskingProperties, realMaskingService);

        AuditEvent event = AuditEvent.builder().eventId(UUID.randomUUID()).eventTime(Instant.now()).eventType("TYPE").action("ACT")
                .beforeSummary("1234567890").build();
        service.record(event, AuditMode.ASYNC_FALLBACK, MaskingTarget.builder().subjectType(SubjectType.CUSTOMER_INDIVIDUAL).build());

        verify(repository).save(any());
    }

    @Test
    void applyMaskReturnsRawWhenMaskingDisabled() {
        given(resolver.resolve(any(), any())).willReturn(Optional.of(AuditPolicySnapshot.builder().enabled(true).maskingEnabled(false).build()));
        AuditRecordService service = new AuditRecordService(repository, resolver, mapper, null, "audit.events.v1", "", null, false, "", "kid1", maskingProperties, null);

        AuditEvent event = AuditEvent.builder().eventId(UUID.randomUUID()).eventTime(Instant.now()).eventType("TYPE").action("ACT")
                .beforeSummary("raw-data").build();
        service.record(event, AuditMode.ASYNC_FALLBACK, MaskingTarget.builder().subjectType(SubjectType.CUSTOMER_INDIVIDUAL).build());

        verify(repository).save(Mockito.argThat(e -> "raw-data".equals(e.getBeforeSummary())));
    }

    @Test
    void inlineMaskableCoversRawAndMasked() {
        // maskingEnabled true + maskingService present, strategy false => raw 사용
        MaskingService rawService = new MaskingService(target -> false);
        given(resolver.resolve(any(), any())).willReturn(Optional.of(AuditPolicySnapshot.builder().enabled(true).build()));
        AuditRecordService serviceRaw = new AuditRecordService(repository, resolver, mapper, null,
                "topic", "", null,
                false, "", "kid", maskingProperties, rawService);
        AuditEvent event = AuditEvent.builder().eventId(UUID.randomUUID()).eventTime(Instant.now()).eventType("TYPE").action("ACT")
                .beforeSummary("SENSITIVE").build();
        serviceRaw.record(event, AuditMode.ASYNC_FALLBACK, MaskingTarget.builder().subjectType(SubjectType.CUSTOMER_INDIVIDUAL).maskRule("FULL").build());

        // maskingEnabled true + strategy true => masked 사용
        MaskingService maskService = new MaskingService(target -> true);
        AuditRecordService serviceMask = new AuditRecordService(repository, resolver, mapper, null,
                "topic", "", null,
                false, "", "kid", maskingProperties, maskService);
        serviceMask.record(event, AuditMode.ASYNC_FALLBACK, MaskingTarget.builder().subjectType(SubjectType.CUSTOMER_INDIVIDUAL).maskRule("FULL").build());
    }
}
