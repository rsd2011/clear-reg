package com.example.common.version;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DiffType 열거형")
class DiffTypeTest {

    @Test
    @DisplayName("Given: DiffType 열거형 / When: values() 호출 / Then: 모든 변경 유형 포함")
    void allValuesExist() {
        DiffType[] values = DiffType.values();

        assertThat(values).containsExactly(
                DiffType.ADDED,
                DiffType.REMOVED,
                DiffType.MODIFIED,
                DiffType.UNCHANGED
        );
    }

    @Test
    @DisplayName("Given: 각 문자열 / When: valueOf() 호출 / Then: 대응하는 열거형 값 반환")
    void valueOfWorksForAllValues() {
        assertThat(DiffType.valueOf("ADDED")).isEqualTo(DiffType.ADDED);
        assertThat(DiffType.valueOf("REMOVED")).isEqualTo(DiffType.REMOVED);
        assertThat(DiffType.valueOf("MODIFIED")).isEqualTo(DiffType.MODIFIED);
        assertThat(DiffType.valueOf("UNCHANGED")).isEqualTo(DiffType.UNCHANGED);
    }

    @Test
    @DisplayName("Given: 각 열거형 값 / When: name() 호출 / Then: 올바른 문자열 반환")
    void nameReturnsCorrectString() {
        assertThat(DiffType.ADDED.name()).isEqualTo("ADDED");
        assertThat(DiffType.REMOVED.name()).isEqualTo("REMOVED");
        assertThat(DiffType.MODIFIED.name()).isEqualTo("MODIFIED");
        assertThat(DiffType.UNCHANGED.name()).isEqualTo("UNCHANGED");
    }

    @Test
    @DisplayName("Given: 각 열거형 값 / When: ordinal() 호출 / Then: 올바른 순서 반환")
    void ordinalReturnsCorrectOrder() {
        assertThat(DiffType.ADDED.ordinal()).isZero();
        assertThat(DiffType.REMOVED.ordinal()).isEqualTo(1);
        assertThat(DiffType.MODIFIED.ordinal()).isEqualTo(2);
        assertThat(DiffType.UNCHANGED.ordinal()).isEqualTo(3);
    }
}
