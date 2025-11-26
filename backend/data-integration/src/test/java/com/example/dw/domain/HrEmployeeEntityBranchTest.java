package com.example.dw.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HrEmployeeEntityBranchTest {

    @Test
    @DisplayName("sameBusinessState는 null/불일치/일치 케이스를 모두 커버한다")
    void sameBusinessStateBranches() {
        HrEmployeeEntity emp = HrEmployeeEntity.snapshot(
                "E1", 1, null, null, null, null, null, null, null, UUID.randomUUID(), java.time.OffsetDateTime.now());

        // 모든 필드 null → true 경로
        assertThat(emp.sameBusinessState(null, null, null, null, null, null, null)).isTrue();

        // left=null vs right!=null → false 분기
        assertThat(emp.sameBusinessState("홍길동", null, null, null, null, null, null)).isFalse();

        // 값 설정 후 완전 일치 → true
        emp = HrEmployeeEntity.snapshot(
                "E1", 2, "홍길동", "hong@example.com", "ORG1", "REGULAR", "ACTIVE",
                LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31),
                UUID.randomUUID(), java.time.OffsetDateTime.now());
        assertThat(emp.sameBusinessState("홍길동", "hong@example.com", "ORG1",
                "REGULAR", "ACTIVE", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31))).isTrue();

        // 한 필드라도 다르면 false
        assertThat(emp.sameBusinessState("홍길동", "hong@example.com", "ORG1",
                "REGULAR", "INACTIVE", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31))).isFalse();

        // closeAt는 시작일 이전을 막고 동일 날짜로 맞춘다
        emp.closeAt(LocalDate.of(2022, 12, 31));
        assertThat(emp.getEffectiveEnd()).isEqualTo(LocalDate.of(2023, 1, 1));

        // closeAt가 시작일 이후면 그대로 설정한다
        emp.closeAt(LocalDate.of(2023, 2, 1));
        assertThat(emp.getEffectiveEnd()).isEqualTo(LocalDate.of(2023, 2, 1));

        // effectiveStart가 null이면 전달된 종료일을 그대로 설정한다
        HrEmployeeEntity noStart = HrEmployeeEntity.snapshot(
                "E2", 1, "이름", "e2@x.com", "ORG", "FULL", "ACTIVE",
                null, null, java.util.UUID.randomUUID(), java.time.OffsetDateTime.now());
        noStart.closeAt(LocalDate.of(2024, 5, 1));
        assertThat(noStart.getEffectiveEnd()).isEqualTo(LocalDate.of(2024, 5, 1));
    }
}
