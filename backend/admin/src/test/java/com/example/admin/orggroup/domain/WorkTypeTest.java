package com.example.admin.orggroup.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.example.common.orggroup.WorkType;

@DisplayName("WorkType enum")
class WorkTypeTest {

    @Test
    @DisplayName("Given: 업무유형 enum이 정의되면 Then: 5개의 값이 존재한다")
    void hasExpectedValues() {
        assertThat(WorkType.values()).containsExactly(
                WorkType.GENERAL,
                WorkType.FILE_EXPORT,
                WorkType.DATA_CORRECTION,
                WorkType.HR_UPDATE,
                WorkType.POLICY_CHANGE
        );
    }

    @ParameterizedTest
    @CsvSource({
            "GENERAL, GENERAL",
            "general, GENERAL",
            "General, GENERAL",
            "FILE_EXPORT, FILE_EXPORT",
            "file_export, FILE_EXPORT",
            "DATA_CORRECTION, DATA_CORRECTION",
            "data_correction, DATA_CORRECTION",
            "HR_UPDATE, HR_UPDATE",
            "hr_update, HR_UPDATE",
            "POLICY_CHANGE, POLICY_CHANGE",
            "policy_change, POLICY_CHANGE"
    })
    @DisplayName("Given: 유효한 코드 문자열이면 When: fromString() 호출 Then: 해당 WorkType 반환")
    void fromStringReturnsWorkType(String input, WorkType expected) {
        assertThat(WorkType.fromString(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "INVALID", "unknown", "file export"})
    @DisplayName("Given: 유효하지 않은 입력이면 When: fromString() 호출 Then: null 반환")
    void fromStringReturnsNullForInvalidInput(String input) {
        assertThat(WorkType.fromString(input)).isNull();
    }
}
