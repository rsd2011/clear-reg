package com.example.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.audit.drm.DrmAuditEvent;
import com.example.audit.drm.DrmEventType;

@DisplayName("감사 DTO equals 브랜치 세밀 커버")
class AuditDtoDeepEqualsTest {

    private AuditEvent baseEvent() {
        return AuditEvent.builder()
                .eventId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
                .eventTime(Instant.parse("2024-01-01T00:00:00Z"))
                .eventType("TYPE").moduleName("MOD").action("ACT")
                .actor(Actor.builder().id("A").type(ActorType.HUMAN).role("R").dept("D").build())
                .subject(Subject.builder().type("SUB").key("KEY").build())
                .channel("CH").clientIp("IP").userAgent("UA").deviceId("DEV")
                .success(true).resultCode("OK").reasonCode("RC").reasonText("RT").legalBasisCode("LB")
                .riskLevel(RiskLevel.LOW)
                .beforeSummary("B").afterSummary("A")
                .extra(Map.of("k", "v"))
                .hashChain("HASH")
                .build();
    }

    @Test
    void auditEventEachFieldMismatch() {
        AuditEvent base = baseEvent();
        assertThat(base).isEqualTo(baseEvent()); // true 경로

        // 각 필드별 false 경로
        assertThat(base).isNotEqualTo(base.toBuilder().eventId(UUID.randomUUID()).build());
        assertThat(base).isNotEqualTo(base.toBuilder().eventTime(Instant.now()).build());
        assertThat(base).isNotEqualTo(base.toBuilder().eventType("X").build());
        assertThat(base).isNotEqualTo(base.toBuilder().moduleName("X").build());
        assertThat(base).isNotEqualTo(base.toBuilder().action("X").build());
        assertThat(base).isNotEqualTo(base.toBuilder().actor(Actor.builder().id("diff").type(ActorType.HUMAN).build()).build());
        assertThat(base).isNotEqualTo(base.toBuilder().subject(Subject.builder().type("SUB").key("DIFF").build()).build());
        assertThat(base).isNotEqualTo(base.toBuilder().channel("X").build());
        assertThat(base).isNotEqualTo(base.toBuilder().clientIp("X").build());
        assertThat(base).isNotEqualTo(base.toBuilder().userAgent("X").build());
        assertThat(base).isNotEqualTo(base.toBuilder().deviceId("X").build());
        assertThat(base).isNotEqualTo(base.toBuilder().success(false).build());
        assertThat(base).isNotEqualTo(base.toBuilder().resultCode("X").build());
        assertThat(base).isNotEqualTo(base.toBuilder().reasonCode("X").build());
        assertThat(base).isNotEqualTo(base.toBuilder().reasonText("X").build());
        assertThat(base).isNotEqualTo(base.toBuilder().legalBasisCode("X").build());
        assertThat(base).isNotEqualTo(base.toBuilder().riskLevel(RiskLevel.HIGH).build());
        assertThat(base).isNotEqualTo(base.toBuilder().beforeSummary("X").build());
        assertThat(base).isNotEqualTo(base.toBuilder().afterSummary("X").build());
        assertThat(base).isNotEqualTo(base.toBuilder().extra(Map.of("k2", "v2")).build());
        assertThat(base).isNotEqualTo(base.toBuilder().hashChain("X").build());

        // null optional 필드 비교
        AuditEvent minimal = AuditEvent.builder().eventType("T").build();
        assertThat(minimal).isNotEqualTo(base);
        assertThat(minimal).isEqualTo(minimal.toBuilder().build());
    }

    @Test
    void actorSubjectPolicySnapshotDrmBranches() {
        Actor a1 = Actor.builder().id("A").type(ActorType.SYSTEM).role("R").dept("D").build();
        Actor a2 = Actor.builder().id("A").type(ActorType.SYSTEM).role("R").dept("D").build();
        Actor aDiff = Actor.builder().id("B").type(ActorType.SYSTEM).build();
        assertThat(a1).isEqualTo(a2);
        assertThat(a1).isNotEqualTo(aDiff);
        assertThat(a1).isNotEqualTo(null);

        Subject s1 = Subject.builder().type("T").key("K").build();
        Subject s2 = Subject.builder().type("T").key("K").build();
        Subject sDiff = Subject.builder().type("T2").key("K2").build();
        assertThat(s1).isEqualTo(s2);
        assertThat(s1).isNotEqualTo(sDiff);

        AuditPolicySnapshot snap = AuditPolicySnapshot.builder()
                .enabled(true).sensitiveApi(true).reasonRequired(true).maskingEnabled(true)
                .mode(AuditMode.ASYNC_FALLBACK).retentionDays(1).riskLevel(RiskLevel.HIGH)
                .attribute("a", "b").build();
        AuditPolicySnapshot snapSame = snap.toBuilder().build();
        AuditPolicySnapshot snapDiff = snap.toBuilder().maskingEnabled(false).build();
        assertThat(snap).isEqualTo(snapSame);
        assertThat(snap).isNotEqualTo(snapDiff);
        DrmAuditEvent drm = DrmAuditEvent.builder().assetId("AS").eventType(DrmEventType.REQUEST).reasonCode("R").reasonText("T")
                .requestorId("REQ").approverId("APP").expiresAt(Instant.now()).route("download").tags(Set.of("tag")).organizationCode("ORG").build();
        DrmAuditEvent drmSame = DrmAuditEvent.builder().assetId("AS").eventType(DrmEventType.REQUEST).reasonCode("R").reasonText("T")
                .requestorId("REQ").approverId("APP").expiresAt(drm.getExpiresAt()).route("download").tags(Set.of("tag")).organizationCode("ORG").build();
        DrmAuditEvent drmDiff = DrmAuditEvent.builder().assetId("AS2").eventType(DrmEventType.APPROVAL).build();
        assertThat(drm).isEqualTo(drmSame);
        assertThat(drm).isNotEqualTo(drmDiff);
        DrmAuditEvent drmTagsNull = DrmAuditEvent.builder().assetId("AS").eventType(DrmEventType.REQUEST).tags(null).build();
        assertThat(drm).isNotEqualTo(drmTagsNull);

        // Actor/Subject null 필드 브랜치
        Actor actorNulls = Actor.builder().id(null).type(null).role(null).dept(null).build();
        assertThat(actorNulls).isNotEqualTo(a1);
        Subject subjectNull = Subject.builder().type(null).key(null).build();
        assertThat(subjectNull).isNotEqualTo(s1);
    }
}
