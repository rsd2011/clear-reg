package com.example.dwgateway.dw;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DwOrganizationRecordTest {

    @Test
    @DisplayName("DwOrganizationRecord는 필드 값을 그대로 보존한다")
    void recordPreservesFields() {
        UUID id = UUID.randomUUID();
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(1);
        OffsetDateTime synced = OffsetDateTime.now();

        DwOrganizationPort.DwOrganizationRecord record = new DwOrganizationPort.DwOrganizationRecord(
                id,
                "ORG",
                1,
                "Organization",
                "PARENT",
                "ACTIVE",
                start,
                end,
                synced
        );

        assertThat(record.id()).isEqualTo(id);
        assertThat(record.organizationCode()).isEqualTo("ORG");
        assertThat(record.version()).isEqualTo(1);
        assertThat(record.name()).isEqualTo("Organization");
        assertThat(record.parentOrganizationCode()).isEqualTo("PARENT");
        assertThat(record.status()).isEqualTo("ACTIVE");
        assertThat(record.effectiveStart()).isEqualTo(start);
        assertThat(record.effectiveEnd()).isEqualTo(end);
        assertThat(record.syncedAt()).isEqualTo(synced);
    }
}
