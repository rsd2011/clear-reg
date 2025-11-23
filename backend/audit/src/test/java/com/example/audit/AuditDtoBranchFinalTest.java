package com.example.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.audit.drm.DrmAuditEvent;
import com.example.audit.drm.DrmEventType;

@DisplayName("감사 DTO 브랜치 최종 커버")
class AuditDtoBranchFinalTest {

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
    void auditEventAllFieldBranches() {
        AuditEvent base = baseEvent();
        // equals true
        assertThat(base).isEqualTo(baseEvent());
        // each field change -> not equal
        Stream.of(
                base.toBuilder().eventId(UUID.randomUUID()).build(),
                base.toBuilder().eventTime(Instant.now()).build(),
                base.toBuilder().eventType("X").build(),
                base.toBuilder().moduleName("X").build(),
                base.toBuilder().action("X").build(),
                base.toBuilder().actor(null).build(),
                base.toBuilder().subject(null).build(),
                base.toBuilder().channel("X").build(),
                base.toBuilder().clientIp("X").build(),
                base.toBuilder().userAgent("X").build(),
                base.toBuilder().deviceId("X").build(),
                base.toBuilder().success(false).build(),
                base.toBuilder().resultCode("X").build(),
                base.toBuilder().reasonCode("X").build(),
                base.toBuilder().reasonText("X").build(),
                base.toBuilder().legalBasisCode("X").build(),
                base.toBuilder().riskLevel(RiskLevel.HIGH).build(),
                base.toBuilder().beforeSummary("X").build(),
                base.toBuilder().afterSummary("X").build(),
                base.toBuilder().extra(Map.of("k2","v2")).build(),
                base.toBuilder().hashChain("X").build()
        ).forEach(diff -> assertThat(base).isNotEqualTo(diff));
        assertThat(base.hashCode()).isEqualTo(baseEvent().hashCode());
        assertThat(base.toString()).contains("TYPE");
    }

    @Test
    void actorSubjectBranches() {
        Actor base = Actor.builder().id("A").type(ActorType.HUMAN).role("R").dept("D").build();
        Actor same = Actor.builder().id("A").type(ActorType.HUMAN).role("R").dept("D").build();
        Actor diff = Actor.builder().id("B").type(ActorType.SYSTEM).build();
        assertThat(base).isEqualTo(same);
        assertThat(base).isNotEqualTo(diff);
        assertThat(base).isNotEqualTo(null);

        Subject s = Subject.builder().type("T").key("K").build();
        Subject sSame = Subject.builder().type("T").key("K").build();
        Subject sDiff = Subject.builder().type("T2").key("K2").build();
        assertThat(s).isEqualTo(sSame);
        assertThat(s).isNotEqualTo(sDiff);
        assertThat(s).isNotEqualTo(null);
    }

    @Test
    void auditPolicySnapshotBranches() {
        AuditPolicySnapshot snap = AuditPolicySnapshot.builder()
                .enabled(true).sensitiveApi(true).reasonRequired(false)
                .maskingEnabled(true).mode(AuditMode.ASYNC_FALLBACK)
                .retentionDays(10).riskLevel(RiskLevel.HIGH)
                .attribute("sensitiveEndpoints", Set.of("/a"))
                .build();
        AuditPolicySnapshot same = snap.toBuilder().build();
        AuditPolicySnapshot diff = snap.toBuilder().enabled(false).build();
        assertThat(snap).isEqualTo(same);
        assertThat(snap).isNotEqualTo(diff);
        assertThat(snap.hashCode()).isEqualTo(same.hashCode());
        assertThat(snap.toString()).contains("riskLevel");
    }

    @Test
    void drmAuditEventBranches() {
        DrmAuditEvent d1 = DrmAuditEvent.builder().assetId("ASSET").eventType(DrmEventType.APPROVAL)
                .reasonCode("R").reasonText("T").requestorId("REQ").approverId("APP").route("download").tags(Set.of("tag")).organizationCode("ORG").build();
        DrmAuditEvent d2 = DrmAuditEvent.builder().assetId("ASSET").eventType(DrmEventType.APPROVAL)
                .reasonCode("R").reasonText("T").requestorId("REQ").approverId("APP").route("download").tags(Set.of("tag")).organizationCode("ORG").build();
        DrmAuditEvent d3 = DrmAuditEvent.builder().assetId("ASSET2").eventType(DrmEventType.EXECUTE).build();
        assertThat(d1).isEqualTo(d2);
        assertThat(d1).isNotEqualTo(d3);
        assertThat(d1).isNotEqualTo(null);
        assertThat(d1).isNotEqualTo("x");
        assertThat(d1).isEqualTo(d1);
        assertThat(d1.hashCode()).isEqualTo(d2.hashCode());
    }
}
