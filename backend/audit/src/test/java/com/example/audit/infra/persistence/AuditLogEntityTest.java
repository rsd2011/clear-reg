package com.example.audit.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AuditLogEntity 생성자/기본값")
class AuditLogEntityTest {

    @Test
    void constructorSetsFields() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        AuditLogEntity entity = new AuditLogEntity(id, now, "TYPE", "mod", "act",
                "actor", "HUMAN", "role", "dept", "SUBJECT", "123",
                "INTERNAL", "127.0.0.1", "UA", "dev-1",
                true, "OK", "R", "T", "PIPA", "LOW",
                "before", "after", "{}", "hash");

        assertThat(entity.getEventId()).isNotNull();
        assertThat(entity.getEventTime()).isEqualTo(now);
        assertThat(entity.getEventType()).isEqualTo("TYPE");
        assertThat(entity.getBeforeSummary()).isEqualTo("before");
        assertThat(entity.getHashChain()).isEqualTo("hash");
    }

    @Test
    void constructorGeneratesIdWhenNull() {
        AuditLogEntity entity = new AuditLogEntity(
                null,               // eventId
                Instant.now(),      // eventTime
                "T",                // eventType
                "m",                // moduleName
                "a",                // action
                null,               // actorId
                null,               // actorType
                null,               // actorRole
                null,               // actorDept
                null,               // subjectType
                null,               // subjectKey
                null,               // channel
                null,               // clientIp
                null,               // userAgent
                null,               // deviceId
                true,               // success
                null,               // resultCode
                null,               // reasonCode
                null,               // reasonText
                null,               // legalBasisCode
                null,               // riskLevel
                null,               // beforeSummary
                null,               // afterSummary
                null,               // extraJson
                null                // hashChain
        );
        assertThat(entity.getEventId()).isNotNull();
        assertThat(entity.getModuleName()).isEqualTo("m");
        assertThat(entity.isSuccess()).isTrue();
        entity.getAction();
        entity.getActorId();
        entity.getBeforeSummary();
        entity.getAfterSummary();
    }
}
