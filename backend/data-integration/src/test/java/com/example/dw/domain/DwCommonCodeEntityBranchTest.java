package com.example.dw.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DwCommonCodeEntityBranchTest {

    @Test
    @DisplayName("setCodeType는 대문자로 변환하고 sameBusinessState 분기를 모두 커버한다")
    void setCodeTypeAndSameBusinessStateBranches() {
        DwCommonCodeEntity entity = DwCommonCodeEntity.create(
                "code_type",
                "001",
                null,
                1,
                true,
                null,
                null,
                null,
                UUID.randomUUID(),
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        assertThat(entity.getCodeType()).isEqualTo("CODE_TYPE");

        // null vs null → true
        assertThat(entity.sameBusinessState(null, 1, true, null, null, null)).isTrue();

        // codeName null vs 값 존재 → false
        assertThat(entity.sameBusinessState("이름", 1, true, null, null, null)).isFalse();

        // 모든 필드 일치 (non-null) → true
        entity.updateFromRecord("이름", 1, true, "CAT", "설명", "{\"key\":\"v\"}", entity.getSourceBatchId(), entity.getSyncedAt());
        assertThat(entity.sameBusinessState("이름", 1, true, "CAT", "설명", "{\"key\":\"v\"}")).isTrue();

        // displayOrder 차이 → false
        assertThat(entity.sameBusinessState("이름", 2, true, "CAT", "설명", "{\"key\":\"v\"}")).isFalse();
    }
}
