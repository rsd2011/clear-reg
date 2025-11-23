package com.example.common.masking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UnmaskAuditEventBranchTest {

    @Test
    @DisplayName("reason/requesterRoles가 null이어도 빌더가 동작한다")
    void nullFieldsAllowed() {
        UnmaskAuditEvent evt = UnmaskAuditEvent.builder()
                .eventTime(Instant.EPOCH)
                .subjectType(SubjectType.UNKNOWN)
                .dataKind("HTTP")
                .fieldName("field")
                .rowId("row")
                .build();

        assertThat(evt.getRequesterRoles()).isNull();
        assertThat(evt.getReason()).isNull();
    }
}
