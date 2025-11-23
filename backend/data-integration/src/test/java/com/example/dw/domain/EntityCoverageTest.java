package com.example.dw.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EntityCoverageTest {

    @Test
    @DisplayName("HrEmployeeEntity sameBusinessState가 필드 비교를 수행한다")
    void hrEmployeeEntityBusinessState() {
        HrEmployeeEntity e = new HrEmployeeEntity();
        e.setFullName("홍길동");
        e.setEmail("a@b.com");
        e.setOrganizationCode("ORG1");
        e.setEmploymentType("FULL");
        e.setEmploymentStatus("ACTIVE");
        e.setEffectiveStart(LocalDate.of(2024, 1, 1));
        e.setEffectiveEnd(LocalDate.of(2024, 12, 31));

        assertThat(e.sameBusinessState("홍길동", "a@b.com", "ORG1", "FULL", "ACTIVE",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))).isTrue();
        assertThat(e.sameBusinessState("홍길동", "x@b.com", "ORG1", "FULL", "ACTIVE",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))).isFalse();
    }

    @Test
    @DisplayName("DwHolidayEntity sameBusinessState가 로컬명/영문명/근무일 여부를 비교한다")
    void dwHolidayEntityBusinessState() {
        DwHolidayEntity h = new DwHolidayEntity();
        h.setLocalName("설날");
        h.setEnglishName("Lunar New Year");
        h.setWorkingDay(false);
        h.setHolidayDate(LocalDate.of(2025, 1, 29));
        h.setCountryCode("KR");
        h.setSyncedAt(OffsetDateTime.of(2024, 12, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        h.setSourceBatchId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        assertThat(h.sameBusinessState("설날", "Lunar New Year", false)).isTrue();
        assertThat(h.sameBusinessState("설날", "Lunar New Year", true)).isFalse();
        assertThat(h.getHolidayDate()).isEqualTo(LocalDate.of(2025, 1, 29));
        assertThat(h.getCountryCode()).isEqualTo("KR");
        assertThat(h.isWorkingDay()).isFalse();
    }

    @Test
    @DisplayName("HrOrganizationStagingEntity getter/setter가 값을 유지한다")
    void hrOrganizationStagingEntityAccessors() {
        HrOrganizationStagingEntity org = new HrOrganizationStagingEntity();
        org.setOrganizationCode("ORG-100");
        org.setName("비밀조직");
        org.setParentOrganizationCode("PARENT");
        org.setStatus("ACTIVE");
        org.setStartDate(LocalDate.of(2024, 2, 1));
        org.setEndDate(LocalDate.of(2024, 12, 31));
        org.setPayloadHash("hash");
        org.setRawPayload("raw");

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
        HrEmployeeStagingEntity stg = new HrEmployeeStagingEntity();
        stg.setEmployeeId("E100");
        stg.setFullName("홍길동");
        stg.setEmail("e@x.com");
        stg.setOrganizationCode("ORG");
        stg.setEmploymentType("FULL");
        stg.setEmploymentStatus("ACTIVE");
        stg.setStartDate(LocalDate.of(2024, 3, 1));
        stg.setEndDate(LocalDate.of(2024, 11, 30));
        stg.setPayloadHash("hash");
        stg.setRawPayload("raw");

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
        HrImportErrorEntity err = new HrImportErrorEntity();
        err.setLineNumber(10);
        err.setRecordType("ORG");
        err.setReferenceCode("REF-1");
        err.setErrorCode("E100");
        err.setErrorMessage("invalid");
        err.setRawPayload("payload");

        assertThat(err.getLineNumber()).isEqualTo(10);
        assertThat(err.getRecordType()).isEqualTo("ORG");
        assertThat(err.getReferenceCode()).isEqualTo("REF-1");
        assertThat(err.getErrorCode()).isEqualTo("E100");
        assertThat(err.getErrorMessage()).isEqualTo("invalid");
        assertThat(err.getRawPayload()).isEqualTo("payload");
    }
}
