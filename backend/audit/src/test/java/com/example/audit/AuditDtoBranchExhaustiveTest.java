package com.example.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.audit.drm.DrmAuditEvent;
import com.example.audit.drm.DrmEventType;

@DisplayName("감사 DTO 브랜치 추가 커버")
class AuditDtoBranchExhaustiveTest {

    @Test
    void equalsFalsePathsForDto() {
        Actor actor = Actor.builder().id("A").type(ActorType.HUMAN).dept("IT").role("R").build();
        assertThat(actor.equals(null)).isFalse();
        assertThat(actor.equals("string")).isFalse();

        Subject subject = Subject.builder().type("SUB").key("KEY").build();
        assertThat(subject.equals(null)).isFalse();
        assertThat(subject.equals("x")).isFalse();

        AuditEvent event = AuditEvent.builder()
                .eventId(UUID.randomUUID())
                .eventTime(Instant.now())
                .eventType("T").moduleName("M").action("A")
                .actor(actor).subject(subject)
                .success(true).riskLevel(RiskLevel.LOW)
                .extra(Map.of("k","v"))
                .build();
        assertThat(event.equals(null)).isFalse();
        assertThat(event.equals("x")).isFalse();
        assertThat(event.toString()).contains("eventType");
        AuditEvent diffId = event.toBuilder().eventId(UUID.randomUUID()).build();
        assertThat(event).isNotEqualTo(diffId);
        AuditEvent diffHash = event.toBuilder().hashChain("h2").build();
        assertThat(event).isNotEqualTo(diffHash);

        AuditPolicySnapshot snap = AuditPolicySnapshot.builder().enabled(true).sensitiveApi(false).build();
        AuditPolicySnapshot snapDiff = snap.toBuilder().enabled(false).build();
        assertThat(snap.equals(null)).isFalse();
        assertThat(snap).isNotEqualTo(snapDiff);
        AuditPolicySnapshot snapMode = snap.toBuilder().mode(AuditMode.ASYNC_FALLBACK).build();
        assertThat(snap).isNotEqualTo(snapMode);

        DrmAuditEvent drm = DrmAuditEvent.builder().assetId("AS").eventType(DrmEventType.REQUEST).build();
        assertThat(drm.equals(null)).isFalse();
        assertThat(drm.equals("x")).isFalse();
        DrmAuditEvent drmDiff = DrmAuditEvent.builder().assetId("AS").eventType(DrmEventType.EXECUTE).build();
        assertThat(drm).isNotEqualTo(drmDiff);
    }
}
