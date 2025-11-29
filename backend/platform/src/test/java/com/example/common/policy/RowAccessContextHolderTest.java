package com.example.common.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.security.RowScope;

class RowAccessContextHolderTest {

    @AfterEach
    void cleanup() {
        RowAccessContextHolder.clear();
    }

    @Test
    @DisplayName("Given: RowAccessMatch 설정 / When: get() 호출 / Then: 동일한 매치 반환")
    void setAndGet() {
        RowAccessMatch match = RowAccessMatch.builder()
                .policyId(UUID.randomUUID())
                .rowScope(RowScope.ORG)
                .priority(1)
                .build();

        RowAccessContextHolder.set(match);

        assertThat(RowAccessContextHolder.get()).isEqualTo(match);
    }

    @Test
    @DisplayName("Given: RowAccessMatch 설정 후 clear / When: get() 호출 / Then: null 반환")
    void clearRemovesMatch() {
        RowAccessMatch match = RowAccessMatch.builder()
                .rowScope(RowScope.ALL)
                .build();

        RowAccessContextHolder.set(match);
        RowAccessContextHolder.clear();

        assertThat(RowAccessContextHolder.get()).isNull();
    }

    @Test
    @DisplayName("Given: 아무것도 설정하지 않음 / When: get() 호출 / Then: null 반환")
    void getReturnsNullWhenNotSet() {
        assertThat(RowAccessContextHolder.get()).isNull();
    }
}
