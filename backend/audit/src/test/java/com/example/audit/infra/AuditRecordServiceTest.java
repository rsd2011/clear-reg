package com.example.audit.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import com.example.audit.infra.masking.MaskingProperties;
import com.example.common.masking.MaskingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.audit.infra.persistence.AuditLogEntity;

class AuditRecordServiceTest {

    AuditLogRepository repository = Mockito.mock(AuditLogRepository.class);
    AuditPolicyResolver policyResolver = Mockito.mock(AuditPolicyResolver.class);
    MaskingProperties maskingProperties = new MaskingProperties();
    KafkaTemplate<String, String> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
    ObjectMapper objectMapper = new ObjectMapper();
    MaskingService maskingService = null;

    @Test
    void record_persistsAndPublishes_whenEnabled() {
        given(policyResolver.resolve(any(), any()))
                .willReturn(Optional.of(AuditPolicySnapshot.builder().enabled(true).build()));
        AuditRecordService service = new AuditRecordService(repository, policyResolver, objectMapper,
                kafkaTemplate, "audit.events.v1", "", kafkaTemplate,
                false, "", "default", maskingProperties, maskingService, null);

        service.record(sampleEvent(), AuditMode.STRICT);

        verify(repository).save(any());
        verify(kafkaTemplate).send(any(), any(), any());
    }

    @Test
    void sanitize_masksSensitiveDigitsInSummariesAndExtra() {
        given(policyResolver.resolve(any(), any()))
                .willReturn(Optional.of(AuditPolicySnapshot.builder().enabled(true).build()));
        AuditRecordService service = new AuditRecordService(repository, policyResolver, objectMapper,
                null, "audit.events.v1", "", null,
                false, "", "default", maskingProperties, maskingService, null);

        AuditEvent event = AuditEvent.builder()
                .eventType("VIEW")
                .beforeSummary("주민번호 990101-1234567, 이름 홍길동, 주소 서울로 123")
                .afterSummary("카드 4111-1111-1111-1111, 계좌 110-123-456789")
                .reasonText("사유 123456789012345")
                .extraEntry("card", "4111111111111111")
                .build();

        service.record(event, AuditMode.ASYNC_FALLBACK);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(repository).save(captor.capture());
        AuditLogEntity saved = captor.getValue();

        assertThat(saved.getBeforeSummary()).contains("[REDACTED-RRN]");
        assertThat(saved.getBeforeSummary()).contains("[REDACTED-NAME]");
        assertThat(saved.getBeforeSummary()).contains("[REDACTED-ADDR]");
        assertThat(saved.getAfterSummary()).contains("[REDACTED-CARD]");
        assertThat(saved.getAfterSummary()).contains("[REDACTED-ACCT]");
        assertThat(saved.getReasonText()).contains("[REDACTED-CARD]");
        assertThat(saved.getExtraJson()).contains("[REDACTED-CARD]");
    }

    @Test
    void policyDisabled_skipsPersistAndPublish() {
        given(policyResolver.resolve(any(), any()))
                .willReturn(Optional.of(AuditPolicySnapshot.builder().enabled(false).build()));
        AuditRecordService service = new AuditRecordService(repository, policyResolver, objectMapper,
                kafkaTemplate, "audit.events.v1", "", kafkaTemplate,
                false, "", "default", maskingProperties, maskingService, null);

        service.record(sampleEvent(), AuditMode.ASYNC_FALLBACK);

        Mockito.verifyNoInteractions(repository);
        Mockito.verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void publishSkippedWhenKafkaTemplateNull() {
        given(policyResolver.resolve(any(), any()))
                .willReturn(Optional.of(AuditPolicySnapshot.builder().enabled(true).build()));
        AuditRecordService service = new AuditRecordService(repository, policyResolver, objectMapper,
                null, "audit.events.v1", "", null,
                false, "", "default", maskingProperties, maskingService, null);

        service.record(sampleEvent(), AuditMode.ASYNC_FALLBACK);

        verify(repository).save(any());
    }

    @Test
    void sanitize_removesRawSensitivePatterns() {
        given(policyResolver.resolve(any(), any()))
                .willReturn(Optional.of(AuditPolicySnapshot.builder().enabled(true).build()));
        AuditRecordService service = new AuditRecordService(repository, policyResolver, objectMapper, null, "audit.events.v1", "", null, false, "", "default", maskingProperties, maskingService, null);

        AuditEvent event = AuditEvent.builder()
                .eventType("VIEW")
                .beforeSummary("RRN 990101-1234567, card 4111-1111-1111-1111")
                .afterSummary("account 110-123-456789")
                .reasonText("주민번호 990101-1234567")
                .extraEntry("acct", "110-123-456789")
                .build();

        service.record(event, AuditMode.ASYNC_FALLBACK);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(repository).save(captor.capture());
        AuditLogEntity saved = captor.getValue();

        String aggregated = String.join(" ", saved.getBeforeSummary(), saved.getAfterSummary(), saved.getReasonText(), saved.getExtraJson());
        // 민감 패턴이 그대로 노출되지 않았는지 확인
        assertThat(aggregated).doesNotContain("990101-1234567");
        assertThat(aggregated).doesNotContain("4111-1111-1111-1111");
        assertThat(aggregated).doesNotContain("110-123-456789");
    }

    @Test
    void sanitize_skipsMaskingWhenPolicyDisablesIt() {
        given(policyResolver.resolve(any(), any()))
                .willReturn(Optional.of(AuditPolicySnapshot.builder().enabled(true).maskingEnabled(false).build()));
        AuditRecordService service = new AuditRecordService(repository, policyResolver, objectMapper, null, "audit.events.v1", "", null, false, "", "default", maskingProperties, maskingService, null);

        AuditEvent event = AuditEvent.builder()
                .eventType("VIEW")
                .beforeSummary("주민번호 990101-1234567")
                .afterSummary("카드 4111-1111-1111-1111")
                .reasonText("계좌 110-123-456789")
                .build();

        service.record(event, AuditMode.ASYNC_FALLBACK);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(repository).save(captor.capture());
        AuditLogEntity saved = captor.getValue();

        assertThat(saved.getBeforeSummary()).contains("990101-1234567");
        assertThat(saved.getAfterSummary()).contains("4111-1111-1111-1111");
        assertThat(saved.getReasonText()).contains("110-123-456789");
    }

    @Test
    void record_skipsWhenPolicyDisabled() {
        given(policyResolver.resolve(any(), any()))
                .willReturn(Optional.of(AuditPolicySnapshot.builder().enabled(false).build()));
        AuditRecordService service = new AuditRecordService(repository, policyResolver, objectMapper, kafkaTemplate, "audit.events.v1", "", null, false, "", "default", maskingProperties, maskingService, null);

        service.record(sampleEvent(), AuditMode.STRICT);

        Mockito.verifyNoInteractions(repository, kafkaTemplate);
    }

    @Test
    void record_strictModeThrowsOnPersistFailure() {
        given(policyResolver.resolve(any(), any()))
                .willReturn(Optional.of(AuditPolicySnapshot.builder().enabled(true).build()));
        given(repository.save(any())).willThrow(new IllegalStateException("fail"));
        AuditRecordService service = new AuditRecordService(repository, policyResolver, objectMapper, kafkaTemplate, "audit.events.v1", "", null, false, "", "default", maskingProperties, maskingService, null);

        assertThatThrownBy(() -> service.record(sampleEvent(), AuditMode.STRICT))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void record_asyncFallbackDoesNotThrowOnPersistFailure() {
        given(policyResolver.resolve(any(), any()))
                .willReturn(Optional.of(AuditPolicySnapshot.builder().enabled(true).build()));
        given(repository.save(any())).willThrow(new IllegalStateException("fail"));
        AuditRecordService service = new AuditRecordService(repository, policyResolver, objectMapper, null, "audit.events.v1", "", null, false, "", "default", maskingProperties, maskingService, null);

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
        AuditRecordService service = new AuditRecordService(repository, policyResolver, objectMapper, null, "audit.events.v1", "", null, false, "", "default", maskingProperties, maskingService, null);

        service.record(sampleEvent(), AuditMode.ASYNC_FALLBACK);

        verify(repository).save(Mockito.argThat(entity -> entity.getHashChain() != null && !entity.getHashChain().isEmpty()));
    }
}
