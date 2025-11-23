package com.example.audit.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AuditLogEntity 모든 getter 커버")
class AuditLogEntityFullGettersTest {

    @Test
    void gettersReturnAssignedValues() {
        AuditLogEntity e = new AuditLogEntity(
                UUID.randomUUID(), Instant.now(), "TYPE", "module", "action",
                "actorId", "HUMAN", "role", "dept", "SUBJECT", "subjKey",
                "INTERNAL", "127.0.0.1", "UA", "dev",
                true, "OK", "RC", "RT", "LEGAL", "HIGH",
                "before", "after", "{}", "hash");

        assertThat(e.getEventType()).isEqualTo("TYPE");
        assertThat(e.getModuleName()).isEqualTo("module");
        assertThat(e.getAction()).isEqualTo("action");
        assertThat(e.getActorId()).isEqualTo("actorId");
        assertThat(e.getActorType()).isEqualTo("HUMAN");
        assertThat(e.getActorRole()).isEqualTo("role");
        assertThat(e.getActorDept()).isEqualTo("dept");
        assertThat(e.getSubjectType()).isEqualTo("SUBJECT");
        assertThat(e.getSubjectKey()).isEqualTo("subjKey");
        assertThat(e.getChannel()).isEqualTo("INTERNAL");
        assertThat(e.getClientIp()).isEqualTo("127.0.0.1");
        assertThat(e.getUserAgent()).isEqualTo("UA");
        assertThat(e.getDeviceId()).isEqualTo("dev");
        assertThat(e.isSuccess()).isTrue();
        assertThat(e.getResultCode()).isEqualTo("OK");
        assertThat(e.getReasonCode()).isEqualTo("RC");
        assertThat(e.getReasonText()).isEqualTo("RT");
        assertThat(e.getLegalBasisCode()).isEqualTo("LEGAL");
        assertThat(e.getRiskLevel()).isEqualTo("HIGH");
        assertThat(e.getBeforeSummary()).isEqualTo("before");
        assertThat(e.getAfterSummary()).isEqualTo("after");
        assertThat(e.getExtraJson()).isEqualTo("{}");
        assertThat(e.getHashChain()).isEqualTo("hash");
    }
}

