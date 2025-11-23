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
        HrEmployeeEntity emp = new HrEmployeeEntity();
        emp.setEmployeeId("E1");
        emp.setFullName(null);
        emp.setEmail(null);
        emp.setOrganizationCode(null);
        emp.setEmploymentType(null);
        emp.setEmploymentStatus(null);
        emp.setEffectiveStart(null);
        emp.setEffectiveEnd(null);
        emp.setSourceBatchId(UUID.randomUUID());

        // 모든 필드 null → true 경로
        assertThat(emp.sameBusinessState(null, null, null, null, null, null, null)).isTrue();

        // left=null vs right!=null → false 분기
        assertThat(emp.sameBusinessState("홍길동", null, null, null, null, null, null)).isFalse();

        // 값 설정 후 완전 일치 → true
        emp.setFullName("홍길동");
        emp.setEmail("hong@example.com");
        emp.setOrganizationCode("ORG1");
        emp.setEmploymentType("REGULAR");
        emp.setEmploymentStatus("ACTIVE");
        emp.setEffectiveStart(LocalDate.of(2023, 1, 1));
        emp.setEffectiveEnd(LocalDate.of(2023, 12, 31));
        assertThat(emp.sameBusinessState("홍길동", "hong@example.com", "ORG1",
                "REGULAR", "ACTIVE", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31))).isTrue();

        // 한 필드라도 다르면 false
        assertThat(emp.sameBusinessState("홍길동", "hong@example.com", "ORG1",
                "REGULAR", "INACTIVE", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31))).isFalse();
    }
}
