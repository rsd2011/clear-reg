package com.example.audit.infra.masking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UnmaskAuditRecordTest {

    @Test
    @DisplayName("UnmaskAuditRecord 필드 세터/게터 동작 확인")
    void gettersSetters() {
        UnmaskAuditRecord record = UnmaskAuditRecord.builder()
                .eventTime(Instant.EPOCH)
                .subjectType(com.example.common.masking.SubjectType.CUSTOMER_INDIVIDUAL)
                .dataKind("SSN")
                .fieldName("residentNo")
                .rowId("row1")
                .requesterRoles("AUDITOR")
                .reason("approved")
                .build();

        assertThat(record.getSubjectType()).isEqualTo(com.example.common.masking.SubjectType.CUSTOMER_INDIVIDUAL);
        assertThat(record.getFieldName()).isEqualTo("residentNo");
        assertThat(record.getReason()).isEqualTo("approved");
        assertThat(record.getEventTime()).isEqualTo(Instant.EPOCH);
    }
}
