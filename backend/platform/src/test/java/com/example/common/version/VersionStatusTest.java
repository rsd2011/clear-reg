package com.example.common.version;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("VersionStatus 열거형")
class VersionStatusTest {

    @Test
    @DisplayName("Given: VersionStatus 열거형 / When: values() 호출 / Then: DRAFT, PUBLISHED, HISTORICAL 포함")
    void allValuesExist() {
        VersionStatus[] values = VersionStatus.values();

        assertThat(values).containsExactly(
                VersionStatus.DRAFT,
                VersionStatus.PUBLISHED,
                VersionStatus.HISTORICAL
        );
    }

    @Test
    @DisplayName("Given: 문자열 'DRAFT' / When: valueOf() 호출 / Then: DRAFT 반환")
    void valueOfDraft() {
        VersionStatus status = VersionStatus.valueOf("DRAFT");
        assertThat(status).isEqualTo(VersionStatus.DRAFT);
    }

    @Test
    @DisplayName("Given: 문자열 'PUBLISHED' / When: valueOf() 호출 / Then: PUBLISHED 반환")
    void valueOfPublished() {
        VersionStatus status = VersionStatus.valueOf("PUBLISHED");
        assertThat(status).isEqualTo(VersionStatus.PUBLISHED);
    }

    @Test
    @DisplayName("Given: 문자열 'HISTORICAL' / When: valueOf() 호출 / Then: HISTORICAL 반환")
    void valueOfHistorical() {
        VersionStatus status = VersionStatus.valueOf("HISTORICAL");
        assertThat(status).isEqualTo(VersionStatus.HISTORICAL);
    }

    @Test
    @DisplayName("Given: 각 열거형 값 / When: name() 호출 / Then: 올바른 문자열 반환")
    void nameReturnsCorrectString() {
        assertThat(VersionStatus.DRAFT.name()).isEqualTo("DRAFT");
        assertThat(VersionStatus.PUBLISHED.name()).isEqualTo("PUBLISHED");
        assertThat(VersionStatus.HISTORICAL.name()).isEqualTo("HISTORICAL");
    }

    @Test
    @DisplayName("Given: 각 열거형 값 / When: ordinal() 호출 / Then: 올바른 순서 반환")
    void ordinalReturnsCorrectOrder() {
        assertThat(VersionStatus.DRAFT.ordinal()).isZero();
        assertThat(VersionStatus.PUBLISHED.ordinal()).isEqualTo(1);
        assertThat(VersionStatus.HISTORICAL.ordinal()).isEqualTo(2);
    }
}
