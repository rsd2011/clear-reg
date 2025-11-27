package com.example.dw.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.dw.dto.DataFeedType;
import com.example.dw.dto.HrOrganizationRecord;

class HrOrganizationStagingEntityTest {

    @Test
    @DisplayName("조직 스테이징 엔티티는 필드 값을 보존한다")
    void storesFields() {
        HrImportBatchEntity batch = HrImportBatchEntity.receive("org.csv", DataFeedType.ORGANIZATION, "SRC",
                java.time.LocalDate.now(), 1, "chk", "/tmp");
        HrOrganizationRecord record = new HrOrganizationRecord("ORG", "조직", "PARENT", "ACTIVE",
                null, null, java.time.LocalDate.now(), null, "raw", 1);
        HrOrganizationStagingEntity entity = HrOrganizationStagingEntity.fromRecord(batch, record, "hash");
        assertThat(entity.getOrganizationCode()).isEqualTo("ORG");
        assertThat(entity.getParentOrganizationCode()).isEqualTo("PARENT");
    }
}
