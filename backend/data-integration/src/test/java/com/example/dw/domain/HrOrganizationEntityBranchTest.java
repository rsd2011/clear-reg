package com.example.dw.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HrOrganizationEntityBranchTest {

    @Test
    @DisplayName("sameBusinessState는 null 필드 비교와 값 불일치 분기를 모두 다룬다")
    void sameBusinessStateBranches() {
        HrOrganizationEntity entity = HrOrganizationEntity.snapshot(
                "ORG1", 1, null, null, null, null, null, UUID.randomUUID(), java.time.OffsetDateTime.now());

        // null vs null → true 분기
        assertThat(entity.sameBusinessState(null, null, null, null, null)).isTrue();

        // left=null, right!=null → false 분기
        assertThat(entity.sameBusinessState("이름", null, null, null, null)).isFalse();

        // left!=null, right 동일 → true 분기
        entity = HrOrganizationEntity.snapshot(
                "ORG1", 2, "이름", "P1", "ACTIVE",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31),
                UUID.randomUUID(), java.time.OffsetDateTime.now());
        assertThat(entity.sameBusinessState("이름", "P1", "ACTIVE",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))).isTrue();

        // left!=null, right!=null but 하나라도 다르면 false 분기
        assertThat(entity.sameBusinessState("다른이름", "P1", "ACTIVE",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))).isFalse();

        // closeAt는 시작일 이전을 방지한다
        entity.closeAt(LocalDate.of(2023, 12, 31));
        assertThat(entity.getEffectiveEnd()).isEqualTo(LocalDate.of(2024, 1, 1));

        // closeAt가 시작일 이후면 그대로 설정된다
        entity.closeAt(LocalDate.of(2024, 2, 1));
        assertThat(entity.getEffectiveEnd()).isEqualTo(LocalDate.of(2024, 2, 1));

        // effectiveStart가 null이면 전달된 날짜를 그대로 설정한다
        HrOrganizationEntity noStart = HrOrganizationEntity.snapshot(
                "ORG2", 1, "Org2", null, "ACTIVE", null, null, UUID.randomUUID(), java.time.OffsetDateTime.now());
        noStart.closeAt(LocalDate.of(2025, 1, 1));
        assertThat(noStart.getEffectiveEnd()).isEqualTo(LocalDate.of(2025, 1, 1));
    }
}
