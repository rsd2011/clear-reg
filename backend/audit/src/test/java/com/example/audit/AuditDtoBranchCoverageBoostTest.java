package com.example.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.audit.drm.DrmAuditEvent;
import com.example.audit.drm.DrmEventType;

@DisplayName("감사 DTO 브랜치 추가 증폭")
class AuditDtoBranchCoverageBoostTest {

    @Test
    void auditEventAllFieldsDifferent() {
        AuditEvent base = AuditEvent.builder()
                .eventId(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"))
                .eventTime(Instant.parse("2024-01-01T00:00:01Z"))
                .eventType("TYPE")
                .moduleName("MOD")
                .action("ACT")
                .actor(Actor.builder().id("A").type(ActorType.HUMAN).role("R").dept("D").build())
                .subject(Subject.builder().type("SUB").key("KEY").build())
                .channel("CH")
                .clientIp("IP")
                .userAgent("UA")
                .deviceId("DEV")
                .success(true)
                .resultCode("OK")
                .reasonCode("RC")
                .reasonText("RT")
                .legalBasisCode("LB")
                .riskLevel(RiskLevel.LOW)
                .beforeSummary("B")
                .afterSummary("A")
                .extra(Map.of("k", "v"))
                .hashChain("HASH")
                .build();

        assertThat(base).isEqualTo(base.toBuilder().build());
        assertThat(base).isNotEqualTo(base.toBuilder().actor(null).build());
        assertThat(base).isNotEqualTo(base.toBuilder().subject(null).build());
        assertThat(base).isNotEqualTo(base.toBuilder().eventType(null).build());
        assertThat(base).isNotEqualTo(base.toBuilder().moduleName(null).build());
        assertThat(base).isNotEqualTo(base.toBuilder().action(null).build());
        assertThat(base).isNotEqualTo(base.toBuilder().channel(null).build());
        assertThat(base).isNotEqualTo(base.toBuilder().clientIp(null).build());
        assertThat(base).isNotEqualTo(base.toBuilder().userAgent(null).build());
        assertThat(base).isNotEqualTo(base.toBuilder().deviceId(null).build());
        assertThat(base).isNotEqualTo(base.toBuilder().resultCode(null).build());
        assertThat(base).isNotEqualTo(base.toBuilder().reasonCode(null).build());
        assertThat(base).isNotEqualTo(base.toBuilder().reasonText(null).build());
        assertThat(base).isNotEqualTo(base.toBuilder().legalBasisCode(null).build());
        assertThat(base).isNotEqualTo(base.toBuilder().beforeSummary(null).build());
        assertThat(base).isNotEqualTo(base.toBuilder().afterSummary(null).build());
        assertThat(base).isNotEqualTo(base.toBuilder().hashChain(null).build());
    }

    @Test
    void actorSubjectNullBranches() {
        Actor actor = Actor.builder().id(null).type(null).role(null).dept(null).build();
        Actor actor2 = Actor.builder().id(null).type(null).role(null).dept(null).build();
        Actor actorDiff = Actor.builder().id("X").type(ActorType.SYSTEM).build();
        assertThat(actor).isEqualTo(actor2);
        assertThat(actor).isNotEqualTo(actorDiff);

        Subject subject = Subject.builder().type(null).key(null).build();
        Subject subject2 = Subject.builder().type(null).key(null).build();
        Subject subjectDiff = Subject.builder().type("T").key("K").build();
        assertThat(subject).isEqualTo(subject2);
        assertThat(subject).isNotEqualTo(subjectDiff);
    }

    @Test
    void auditPolicySnapshotFieldDifferences() {
        AuditPolicySnapshot base = AuditPolicySnapshot.builder()
                .enabled(true).sensitiveApi(true).reasonRequired(true).maskingEnabled(true)
                .mode(AuditMode.STRICT).retentionDays(10).riskLevel(RiskLevel.MEDIUM)
                .attribute("sensitiveEndpoints", Set.of("/a")).build();

        assertThat(base).isEqualTo(base.toBuilder().build());
        assertThat(base).isNotEqualTo(base.toBuilder().enabled(false).build());
        assertThat(base).isNotEqualTo(base.toBuilder().sensitiveApi(false).build());
        assertThat(base).isNotEqualTo(base.toBuilder().reasonRequired(false).build());
        assertThat(base).isNotEqualTo(base.toBuilder().maskingEnabled(false).build());
        assertThat(base).isNotEqualTo(base.toBuilder().mode(AuditMode.ASYNC_FALLBACK).build());
        assertThat(base).isNotEqualTo(base.toBuilder().retentionDays(99).build());
        assertThat(base).isNotEqualTo(base.toBuilder().riskLevel(RiskLevel.HIGH).build());
        assertThat(base).isNotEqualTo(base.toBuilder().attributes(Map.of("x","y")).build());
    }

    @Test
    void drmAuditEventAllFieldsDifferences() {
        DrmAuditEvent base = DrmAuditEvent.builder()
                .assetId("AS")
                .eventType(DrmEventType.REQUEST)
                .reasonCode("RC")
                .reasonText("RT")
                .requestorId("REQ")
                .approverId("APP")
                .expiresAt(Instant.parse("2024-01-01T00:00:00Z"))
                .route("download")
                .tags(Set.of("tag"))
                .organizationCode("ORG")
                .build();

        DrmAuditEvent same = DrmAuditEvent.builder()
                .assetId("AS").eventType(DrmEventType.REQUEST).reasonCode("RC").reasonText("RT")
                .requestorId("REQ").approverId("APP").expiresAt(Instant.parse("2024-01-01T00:00:00Z"))
                .route("download").tags(Set.of("tag")).organizationCode("ORG").build();
        assertThat(base).isEqualTo(same);

        assertThat(base).isNotEqualTo(DrmAuditEvent.builder().assetId("DIFF").eventType(DrmEventType.REQUEST).build());
        assertThat(base).isNotEqualTo(DrmAuditEvent.builder().assetId("AS").eventType(DrmEventType.APPROVAL).build());
        assertThat(base).isNotEqualTo(DrmAuditEvent.builder().assetId("AS").eventType(DrmEventType.REQUEST).reasonCode("X").build());
        assertThat(base).isNotEqualTo(DrmAuditEvent.builder().assetId("AS").eventType(DrmEventType.REQUEST).reasonText("X").build());
        assertThat(base).isNotEqualTo(DrmAuditEvent.builder().assetId("AS").eventType(DrmEventType.REQUEST).requestorId("X").build());
        assertThat(base).isNotEqualTo(DrmAuditEvent.builder().assetId("AS").eventType(DrmEventType.REQUEST).approverId("X").build());
        assertThat(base).isNotEqualTo(DrmAuditEvent.builder().assetId("AS").eventType(DrmEventType.REQUEST).expiresAt(Instant.now()).build());
        assertThat(base).isNotEqualTo(DrmAuditEvent.builder().assetId("AS").eventType(DrmEventType.REQUEST).route("mail").build());
        assertThat(base).isNotEqualTo(DrmAuditEvent.builder().assetId("AS").eventType(DrmEventType.REQUEST).tags(Set.of("t2")).build());
        assertThat(base).isNotEqualTo(DrmAuditEvent.builder().assetId("AS").eventType(DrmEventType.REQUEST).organizationCode("DIFF").build());
    }
}
