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
        HrOrganizationEntity entity = new HrOrganizationEntity();
        entity.setOrganizationCode("ORG1");
        entity.setName(null); // left=null 분기
        entity.setParentOrganizationCode(null);
        entity.setStatus(null);
        entity.setEffectiveStart(null);
        entity.setEffectiveEnd(null);
        entity.setSourceBatchId(UUID.randomUUID());

        // null vs null → true 분기
        assertThat(entity.sameBusinessState(null, null, null, null, null)).isTrue();

        // left=null, right!=null → false 분기
        assertThat(entity.sameBusinessState("이름", null, null, null, null)).isFalse();

        // left!=null, right 동일 → true 분기
        entity.setName("이름");
        entity.setParentOrganizationCode("P1");
        entity.setStatus("ACTIVE");
        entity.setEffectiveStart(LocalDate.of(2024, 1, 1));
        entity.setEffectiveEnd(LocalDate.of(2024, 12, 31));
        assertThat(entity.sameBusinessState("이름", "P1", "ACTIVE",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))).isTrue();

        // left!=null, right!=null but 하나라도 다르면 false 분기
        assertThat(entity.sameBusinessState("다른이름", "P1", "ACTIVE",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))).isFalse();
    }
}
