package com.example.dw.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.dw.dto.DataFeedType;
import com.example.dw.dto.HrEmployeeRecord;
import com.example.dw.dto.HrOrganizationRecord;

class EntityCoverageTest {

    @Test
    @DisplayName("HrEmployeeEntity sameBusinessState가 필드 비교를 수행한다")
    void hrEmployeeEntityBusinessState() {
        HrEmployeeEntity e = HrEmployeeEntity.snapshot(
                "E1", 1, "홍길동", "a@b.com", "ORG1", "FULL", "ACTIVE",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31),
                java.util.UUID.randomUUID(), java.time.OffsetDateTime.now());

        assertThat(e.sameBusinessState("홍길동", "a@b.com", "ORG1", "FULL", "ACTIVE",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))).isTrue();
        assertThat(e.sameBusinessState("홍길동", "x@b.com", "ORG1", "FULL", "ACTIVE",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))).isFalse();
    }

    @Test
    @DisplayName("DwHolidayEntity sameBusinessState가 로컬명/영문명/근무일 여부를 비교한다")
    void dwHolidayEntityBusinessState() {
        DwHolidayEntity h = DwHolidayEntity.create(
                LocalDate.of(2025, 1, 29),
                "KR",
                "설날",
                "Lunar New Year",
                false,
                UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                OffsetDateTime.of(2024, 12, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        );

        assertThat(h.sameBusinessState("설날", "Lunar New Year", false)).isTrue();
        assertThat(h.sameBusinessState("설날", "Lunar New Year", true)).isFalse();
        assertThat(h.getHolidayDate()).isEqualTo(LocalDate.of(2025, 1, 29));
        assertThat(h.getCountryCode()).isEqualTo("KR");
        assertThat(h.isWorkingDay()).isFalse();
    }

    @Test
    @DisplayName("HrOrganizationStagingEntity getter/setter가 값을 유지한다")
    void hrOrganizationStagingEntityAccessors() {
        HrImportBatchEntity batch = HrImportBatchEntity.receive("f.csv", DataFeedType.ORGANIZATION, "SRC",
                LocalDate.of(2024, 2, 1), 1, "chk", "/tmp/f.csv");
        HrOrganizationRecord record = new HrOrganizationRecord("ORG-100", "비밀조직", "PARENT", "ACTIVE",
                null, null, LocalDate.of(2024, 2, 1), LocalDate.of(2024, 12, 31), "raw", 1);
        HrOrganizationStagingEntity org = HrOrganizationStagingEntity.fromRecord(batch, record, "hash");

        assertThat(org.getOrganizationCode()).isEqualTo("ORG-100");
        assertThat(org.getName()).isEqualTo("비밀조직");
        assertThat(org.getParentOrganizationCode()).isEqualTo("PARENT");
        assertThat(org.getStatus()).isEqualTo("ACTIVE");
        assertThat(org.getStartDate()).isEqualTo(LocalDate.of(2024, 2, 1));
        assertThat(org.getEndDate()).isEqualTo(LocalDate.of(2024, 12, 31));
        assertThat(org.getPayloadHash()).isEqualTo("hash");
        assertThat(org.getRawPayload()).isEqualTo("raw");
    }

    @Test
    @DisplayName("HrEmployeeStagingEntity getter/setter가 값을 유지한다")
    void hrEmployeeStagingEntityAccessors() {
        HrImportBatchEntity batch = HrImportBatchEntity.receive("f.csv", DataFeedType.EMPLOYEE, "SRC",
                LocalDate.of(2024, 3, 1), 1, "chk", "/tmp/f.csv");
        HrEmployeeRecord record = new HrEmployeeRecord("E100", "홍길동", "e@x.com", "ORG",
                "FULL", "ACTIVE", LocalDate.of(2024, 3, 1), LocalDate.of(2024, 11, 30), "raw", 2);
        HrEmployeeStagingEntity stg = HrEmployeeStagingEntity.fromRecord(batch, record, "hash");

        assertThat(stg.getEmployeeId()).isEqualTo("E100");
        assertThat(stg.getFullName()).isEqualTo("홍길동");
        assertThat(stg.getEmail()).isEqualTo("e@x.com");
        assertThat(stg.getOrganizationCode()).isEqualTo("ORG");
        assertThat(stg.getEmploymentType()).isEqualTo("FULL");
        assertThat(stg.getEmploymentStatus()).isEqualTo("ACTIVE");
        assertThat(stg.getStartDate()).isEqualTo(LocalDate.of(2024, 3, 1));
        assertThat(stg.getEndDate()).isEqualTo(LocalDate.of(2024, 11, 30));
        assertThat(stg.getPayloadHash()).isEqualTo("hash");
        assertThat(stg.getRawPayload()).isEqualTo("raw");
    }

    @Test
    @DisplayName("HrImportErrorEntity getter/setter가 값을 유지한다")
    void hrImportErrorEntityAccessors() {
        HrImportBatchEntity batch = HrImportBatchEntity.receive("f.csv", DataFeedType.EMPLOYEE, "SRC",
                LocalDate.of(2024, 1, 1), 1, "chk", "/tmp");
        HrImportErrorEntity err = HrImportErrorEntity.of(batch, 10, "ORG", "REF-1", "E100", "invalid", "payload");

        assertThat(err.getLineNumber()).isEqualTo(10);
        assertThat(err.getRecordType()).isEqualTo("ORG");
        assertThat(err.getReferenceCode()).isEqualTo("REF-1");
        assertThat(err.getErrorCode()).isEqualTo("E100");
        assertThat(err.getErrorMessage()).isEqualTo("invalid");
        assertThat(err.getRawPayload()).isEqualTo("payload");
    }
}
