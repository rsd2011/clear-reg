package com.example.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AuditEventTest {

    @Test
    void defaults_areApplied() {
        var event = AuditEvent.builder()
                .eventType("LOGIN")
                .build();

        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getEventTime()).isBeforeOrEqualTo(Instant.now());
        assertThat(event.getRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(event.isSuccess()).isTrue();
    }

    @Test
    void builder_setsAllFields() {
        var actor = Actor.builder().id("user1").type(ActorType.HUMAN).role("ADMIN").dept("IT").build();
        var subject = Subject.builder().type("ACCOUNT").key("12345").build();

        var event = AuditEvent.builder()
                .eventType("VIEW")
                .moduleName("auth")
                .action("LOGIN")
                .actor(actor)
                .subject(subject)
                .channel("INTERNAL")
                .clientIp("10.0.0.1")
                .userAgent("JUnit")
                .deviceId("device-1")
                .success(false)
                .resultCode("ERR-01")
                .reasonCode("R001")
                .reasonText("테스트")
                .legalBasisCode("PIPA")
                .riskLevel(RiskLevel.HIGH)
                .beforeSummary("before")
                .afterSummary("after")
                .extraEntry("foo", "bar")
                .hashChain("hash")
                .build();

        assertThat(event.getActor()).isEqualTo(actor);
        assertThat(event.getSubject()).isEqualTo(subject);
        assertThat(event.getChannel()).isEqualTo("INTERNAL");
        assertThat(event.getClientIp()).isEqualTo("10.0.0.1");
        assertThat(event.getUserAgent()).isEqualTo("JUnit");
        assertThat(event.getDeviceId()).isEqualTo("device-1");
        assertThat(event.isSuccess()).isFalse();
        assertThat(event.getResultCode()).isEqualTo("ERR-01");
        assertThat(event.getReasonCode()).isEqualTo("R001");
        assertThat(event.getReasonText()).isEqualTo("테스트");
        assertThat(event.getLegalBasisCode()).isEqualTo("PIPA");
        assertThat(event.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(event.getBeforeSummary()).isEqualTo("before");
        assertThat(event.getAfterSummary()).isEqualTo("after");
        assertThat(event.getExtra()).containsEntry("foo", "bar");
        assertThat(event.getHashChain()).isEqualTo("hash");
    }
}
