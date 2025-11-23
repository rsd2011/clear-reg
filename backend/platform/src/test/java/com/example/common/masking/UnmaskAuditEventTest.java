package com.example.common.masking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("언마스킹 감사 이벤트 커버리지")
class UnmaskAuditEventTest {

    @Test
    @DisplayName("빌더와 equals/hashCode 실행")
    void builderAndEquality() {
        Instant now = Instant.now();
        UnmaskAuditEvent event = UnmaskAuditEvent.builder()
                .eventTime(now)
                .subjectType(SubjectType.CUSTOMER_INDIVIDUAL)
                .dataKind("RRN")
                .fieldName("residentNumber")
                .rowId("row-1")
                .requesterRoles(Set.of("AUDIT_ADMIN"))
                .reason("승인됨")
                .build();

        UnmaskAuditEvent same = UnmaskAuditEvent.builder()
                .eventTime(now)
                .subjectType(SubjectType.CUSTOMER_INDIVIDUAL)
                .dataKind("RRN")
                .fieldName("residentNumber")
                .rowId("row-1")
                .requesterRoles(Set.of("AUDIT_ADMIN"))
                .reason("승인됨")
                .build();

        assertThat(event).isEqualTo(same);
        assertThat(event.hashCode()).isEqualTo(same.hashCode());
        assertThat(event.getRequesterRoles()).contains("AUDIT_ADMIN");
        assertThat(event).isNotEqualTo(null);
        assertThat(event).isNotEqualTo("string");

        UnmaskAuditEvent diffReason = UnmaskAuditEvent.builder()
                .eventTime(now)
                .subjectType(SubjectType.CUSTOMER_INDIVIDUAL)
                .dataKind("CARD")
                .fieldName("residentNumber")
                .rowId("row-1")
                .requesterRoles(Set.of("ANOTHER"))
                .reason("다름")
                .build();
        assertThat(event).isNotEqualTo(diffReason);

        UnmaskAuditEvent nullEvent = UnmaskAuditEvent.builder().build();
        // hashCode/equals null 필드 브랜치
        nullEvent.hashCode();
        assertThat(nullEvent).isNotEqualTo(event);
    }
}
