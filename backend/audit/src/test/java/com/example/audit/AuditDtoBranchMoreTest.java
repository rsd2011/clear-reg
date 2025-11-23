package com.example.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.audit.drm.DrmAuditEvent;
import com.example.audit.drm.DrmEventType;

@DisplayName("감사 DTO 추가 브랜치 커버")
class AuditDtoBranchMoreTest {

    @Test
    void auditEventEqualsBranches() {
        AuditEvent base = AuditEvent.builder()
                .eventId(UUID.randomUUID())
                .eventTime(Instant.now())
                .eventType("TYPE")
                .moduleName("mod")
                .action("act")
                .success(false)
                .riskLevel(RiskLevel.HIGH)
                .hashChain("hash")
                .extra(Map.of("k", "v"))
                .build();
        AuditEvent same = base.toBuilder().build();
        AuditEvent different = base.toBuilder().resultCode("DIFF").build();
        assertThat(base).isEqualTo(same);
        assertThat(base).isNotEqualTo(different);
        assertThat(base.hashCode()).isEqualTo(same.hashCode());
    }

    @Test
    void actorSubjectHashCodeBranches() {
        Actor actor = Actor.builder().id("A").type(ActorType.SYSTEM).build();
        Actor actor2 = Actor.builder().id("A").type(ActorType.SYSTEM).build();
        Actor actorDiff = Actor.builder().id("B").type(ActorType.HUMAN).dept("HR").role("R").build();
        assertThat(actor).isEqualTo(actor2);
        assertThat(actor).isNotEqualTo(actorDiff);
        assertThat(actor.hashCode()).isEqualTo(actor2.hashCode());

        Subject subject = Subject.builder().type("T").key("K").build();
        Subject subject2 = Subject.builder().type("T").key("K").build();
        Subject subjectDiff = Subject.builder().type("T2").key("K2").build();
        assertThat(subject).isEqualTo(subject2);
        assertThat(subject).isNotEqualTo(subjectDiff);
    }

    @Test
    void auditPolicySnapshotEqualsBranches() {
        AuditPolicySnapshot a = AuditPolicySnapshot.builder()
                .enabled(false)
                .sensitiveApi(true)
                .reasonRequired(false)
                .mode(AuditMode.ASYNC_FALLBACK)
                .retentionDays(10)
                .riskLevel(RiskLevel.HIGH)
                .maskingEnabled(false)
                .attribute("x", "y")
                .build();
        AuditPolicySnapshot b = a.toBuilder().build();
        AuditPolicySnapshot c = a.toBuilder().enabled(true).build();
        assertThat(a).isEqualTo(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void drmAuditEventEqualsBranches() {
        DrmAuditEvent d1 = DrmAuditEvent.builder().assetId("ASSET").eventType(DrmEventType.APPROVAL).reasonCode("R").build();
        DrmAuditEvent d2 = DrmAuditEvent.builder().assetId("ASSET").eventType(DrmEventType.APPROVAL).reasonCode("R").build();
        DrmAuditEvent d3 = DrmAuditEvent.builder().assetId("ASSET2").eventType(DrmEventType.EXECUTE).build();
        assertThat(d1).isEqualTo(d2);
        assertThat(d1).isNotEqualTo(d3);
    }
}

