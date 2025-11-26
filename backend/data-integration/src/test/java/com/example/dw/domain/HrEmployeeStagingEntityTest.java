package com.example.dw.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.dw.dto.DataFeedType;
import com.example.dw.dto.HrEmployeeRecord;

class HrEmployeeStagingEntityTest {

    @Test
    @DisplayName("스테이징 엔티티는 기본 필드를 보존하고 비교 가능하다")
    void stagingEntityStoresFields() {
        HrImportBatchEntity batch = HrImportBatchEntity.receive("emp.csv", DataFeedType.EMPLOYEE, "SRC",
                LocalDate.now(), 1, "chk", "/tmp");
        HrEmployeeRecord record = new HrEmployeeRecord("E-STAGE", "홍길동", "e@x.com", "ORG",
                "FULL", "ACTIVE", LocalDate.now(), null, "raw", 1);
        HrEmployeeStagingEntity entity = HrEmployeeStagingEntity.fromRecord(batch, record, "hash");
        assertThat(entity.getEmployeeId()).isEqualTo("E-STAGE");
        assertThat(entity.getFullName()).isEqualTo("홍길동");
        assertThat(entity.getOrganizationCode()).isEqualTo("ORG");
        assertThat(entity.getEmploymentStatus()).isEqualTo("ACTIVE");
    }
}
