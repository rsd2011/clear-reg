package com.example.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.audit.drm.DrmAuditEvent;

@DisplayName("감사 DTO/빌더 브랜치 커버리지")
class AuditDtoCoverageTest {

    @Test
    void actorSubjectAuditEventBuilders() {
        Actor actor = Actor.builder().id("u1").type(ActorType.HUMAN).role("ADMIN").dept("IT").build();
        Subject subject = Subject.builder().type("CUSTOMER").key("C1").build();

        AuditEvent event = AuditEvent.builder()
                .eventId(UUID.randomUUID())
                .eventTime(Instant.now())
                .eventType("LOGIN")
                .moduleName("auth")
                .action("SUCCESS")
                .actor(actor)
                .subject(subject)
                .channel("INTERNAL")
                .clientIp("127.0.0.1")
                .userAgent("JUnit")
                .deviceId("dev")
                .success(true)
                .resultCode("OK")
                .reasonCode("RC")
                .reasonText("because")
                .legalBasisCode("PIPA")
                .riskLevel(RiskLevel.LOW)
                .beforeSummary("before")
                .afterSummary("after")
                .extra(Map.of("k", "v"))
                .build();

        assertThat(event.getActor().getId()).isEqualTo("u1");
        assertThat(event.getSubject().getKey()).isEqualTo("C1");
        assertThat(event.getExtra()).containsEntry("k", "v");
        AuditEvent cloned = event.toBuilder().success(false).build();
        assertThat(cloned.isSuccess()).isFalse();
    }

    @Test
    void auditPolicySnapshotBuilderDefaults() {
        AuditPolicySnapshot snap = AuditPolicySnapshot.builder().build();
        assertThat(snap.isEnabled()).isTrue();
        assertThat(snap.isMaskingEnabled()).isTrue();
        assertThat(snap.getMode()).isEqualTo(AuditMode.STRICT);

        AuditPolicySnapshot async = AuditPolicySnapshot.builder().enabled(false).maskingEnabled(false).mode(AuditMode.ASYNC_FALLBACK).retentionDays(10).riskLevel(RiskLevel.HIGH).build();
        assertThat(async.isEnabled()).isFalse();
        assertThat(async.isMaskingEnabled()).isFalse();
        assertThat(async.getMode()).isEqualTo(AuditMode.ASYNC_FALLBACK);
        assertThat(async.getRetentionDays()).isEqualTo(10);
        assertThat(async.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void drmAuditEventBuilder() {
        DrmAuditEvent drm = DrmAuditEvent.builder()
                .assetId("FILE1")
                .eventType(com.example.audit.drm.DrmEventType.REQUEST)
                .reasonCode("RC")
                .reasonText("approved")
                .requestorId("req1")
                .approverId("appr1")
                .expiresAt(Instant.now().plusSeconds(3600))
                .route("download")
                .organizationCode("ORG")
                .tags(Set.of("excel", "temp"))
                .build();
        assertThat(drm.getEventType()).isEqualTo(com.example.audit.drm.DrmEventType.REQUEST);
        assertThat(drm.getTags()).contains("excel");
    }
}
