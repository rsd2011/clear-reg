package com.example.admin.orggroup.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.example.common.orggroup.WorkCategory;

/**
 * WorkCategorySetConverter 테스트.
 *
 * <p>공통 변환 로직은 AbstractEnumSetJsonConverterTest에서 테스트됨.
 * 이 테스트는 WorkCategory 특화 동작만 검증.
 */
@DisplayName("WorkCategorySetConverter 테스트")
class WorkCategorySetConverterTest {

    private final WorkCategorySetConverter converter = new WorkCategorySetConverter();

    @ParameterizedTest
    @EnumSource(WorkCategory.class)
    @DisplayName("모든 WorkCategory 값의 양방향 변환이 정상 동작한다")
    void roundTripAllWorkCategories(WorkCategory category) {
        Set<WorkCategory> original = EnumSet.of(category);

        String json = converter.convertToDatabaseColumn(original);
        Set<WorkCategory> restored = converter.convertToEntityAttribute(json);

        assertThat(restored).containsExactly(category);
    }

    @Test
    @DisplayName("모든 WorkCategory를 포함한 Set의 양방향 변환")
    void roundTripAllCategories() {
        Set<WorkCategory> original = EnumSet.allOf(WorkCategory.class);

        String json = converter.convertToDatabaseColumn(original);
        Set<WorkCategory> restored = converter.convertToEntityAttribute(json);

        assertThat(restored).containsExactlyInAnyOrderElementsOf(original);
    }
}
