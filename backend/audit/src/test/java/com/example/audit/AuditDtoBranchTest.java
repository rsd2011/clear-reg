package com.example.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.audit.drm.DrmAuditEvent;
import com.example.audit.drm.DrmEventType;

@DisplayName("감사 DTO equals/hashCode 브랜치")
class AuditDtoBranchTest {

    @Test
    void actorSubjectEqualsBranches() {
        Actor a1 = Actor.builder().id("u1").type(ActorType.HUMAN).role("ADMIN").dept("IT").build();
        Actor a2 = Actor.builder().id("u1").type(ActorType.HUMAN).role("ADMIN").dept("IT").build();
        Actor a3 = Actor.builder().id("u2").type(ActorType.SYSTEM).build();

        assertThat(a1).isEqualTo(a2);
        assertThat(a1).isNotEqualTo(a3);
        assertThat(a1).isNotEqualTo(null);

        Subject s1 = Subject.builder().type("CUSTOMER").key("C1").build();
        Subject s2 = Subject.builder().type("CUSTOMER").key("C1").build();
        Subject s3 = Subject.builder().type("EMP").key("E1").build();
        assertThat(s1).isEqualTo(s2);
        assertThat(s1).isNotEqualTo(s3);
    }

    @Test
    void auditEventEqualsBranches() {
        AuditEvent e1 = AuditEvent.builder().eventId(UUID.randomUUID()).eventTime(Instant.now()).eventType("T").action("A").build();
        AuditEvent e2 = e1.toBuilder().build();
        AuditEvent e3 = e1.toBuilder().action("B").build();

        assertThat(e1).isEqualTo(e2);
        assertThat(e1).isNotEqualTo(e3);
        assertThat(e1).isNotEqualTo(null);
    }

    @Test
    void auditPolicySnapshotEqualsBranches() {
        AuditPolicySnapshot p1 = AuditPolicySnapshot.builder().enabled(true).sensitiveApi(true).reasonRequired(true).maskingEnabled(true).mode(AuditMode.ASYNC_FALLBACK).riskLevel(RiskLevel.HIGH).attributes(Map.of("k","v")).build();
        AuditPolicySnapshot p2 = p1.toBuilder().build();
        AuditPolicySnapshot p3 = p1.toBuilder().enabled(false).build();

        assertThat(p1).isEqualTo(p2);
        assertThat(p1).isNotEqualTo(p3);
    }

    @Test
    void drmAuditEventEqualsBranches() {
        DrmAuditEvent d1 = DrmAuditEvent.builder().assetId("F1").eventType(DrmEventType.REQUEST).reasonCode("R").build();
        DrmAuditEvent d2 = DrmAuditEvent.builder().assetId("F1").eventType(DrmEventType.REQUEST).reasonCode("R").build();
        DrmAuditEvent d3 = DrmAuditEvent.builder().assetId("F2").eventType(DrmEventType.APPROVAL).build();

        assertThat(d1).isEqualTo(d2);
        assertThat(d1).isNotEqualTo(d3);
    }
}

