package com.example.admin.permission.domain;

import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PermissionAssignmentListConverter 테스트")
class PermissionAssignmentListConverterTest {

    private PermissionAssignmentListConverter converter;

    @BeforeEach
    void setUp() {
        converter = new PermissionAssignmentListConverter();
    }

    @Nested
    @DisplayName("convertToDatabaseColumn 메서드")
    class ConvertToDatabaseColumnTests {

        @Test
        @DisplayName("Given null 리스트 When 변환 Then 빈 JSON 배열 반환")
        void nullListReturnsEmptyArray() {
            String result = converter.convertToDatabaseColumn(null);
            assertThat(result).isEqualTo("[]");
        }

        @Test
        @DisplayName("Given 빈 리스트 When 변환 Then 빈 JSON 배열 반환")
        void emptyListReturnsEmptyArray() {
            String result = converter.convertToDatabaseColumn(new ArrayList<>());
            assertThat(result).isEqualTo("[]");
        }

        @Test
        @DisplayName("Given 할당 목록 When 변환 Then JSON 배열로 직렬화")
        void assignmentsToJson() {
            List<PermissionAssignment> assignments = List.of(
                    new PermissionAssignment(FeatureCode.DRAFT, ActionCode.DRAFT_CREATE),
                    new PermissionAssignment(FeatureCode.NOTICE, ActionCode.READ)
            );

            String result = converter.convertToDatabaseColumn(assignments);

            assertThat(result).contains("\"feature\":\"DRAFT\"");
            assertThat(result).contains("\"action\":\"DRAFT_CREATE\"");
            assertThat(result).contains("\"feature\":\"NOTICE\"");
            assertThat(result).contains("\"action\":\"READ\"");
        }
    }

    @Nested
    @DisplayName("convertToEntityAttribute 메서드")
    class ConvertToEntityAttributeTests {

        @Test
        @DisplayName("Given null 문자열 When 변환 Then 빈 리스트 반환")
        void nullStringReturnsEmptyList() {
            List<PermissionAssignment> result = converter.convertToEntityAttribute(null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given 빈 문자열 When 변환 Then 빈 리스트 반환")
        void blankStringReturnsEmptyList() {
            List<PermissionAssignment> result = converter.convertToEntityAttribute("   ");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given 빈 JSON 배열 When 변환 Then 빈 리스트 반환")
        void emptyArrayReturnsEmptyList() {
            List<PermissionAssignment> result = converter.convertToEntityAttribute("[]");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given 유효한 JSON When 변환 Then 할당 목록 반환")
        void validJsonToAssignments() {
            String json = "[{\"feature\":\"DRAFT\",\"action\":\"DRAFT_CREATE\"},{\"feature\":\"NOTICE\",\"action\":\"READ\"}]";

            List<PermissionAssignment> result = converter.convertToEntityAttribute(json);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getFeature()).isEqualTo(FeatureCode.DRAFT);
            assertThat(result.get(0).getAction()).isEqualTo(ActionCode.DRAFT_CREATE);
            assertThat(result.get(1).getFeature()).isEqualTo(FeatureCode.NOTICE);
            assertThat(result.get(1).getAction()).isEqualTo(ActionCode.READ);
        }

        @Test
        @DisplayName("Given 잘못된 JSON When 변환 Then IllegalArgumentException 발생")
        void invalidJsonThrows() {
            String invalidJson = "not a valid json";

            assertThatThrownBy(() -> converter.convertToEntityAttribute(invalidJson))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Failed to convert JSON to assignments");
        }

        @Test
        @DisplayName("Given 존재하지 않는 FeatureCode When 변환 Then IllegalArgumentException 발생")
        void invalidFeatureCodeThrows() {
            String json = "[{\"feature\":\"INVALID_FEATURE\",\"action\":\"READ\"}]";

            assertThatThrownBy(() -> converter.convertToEntityAttribute(json))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("왕복 변환 테스트")
    class RoundTripTests {

        @Test
        @DisplayName("Given 할당 목록 When DB 저장 후 조회 Then 원본과 동일")
        void roundTripPreservesData() {
            List<PermissionAssignment> original = List.of(
                    new PermissionAssignment(FeatureCode.DRAFT, ActionCode.DRAFT_CREATE),
                    new PermissionAssignment(FeatureCode.DRAFT, ActionCode.DRAFT_SUBMIT),
                    new PermissionAssignment(FeatureCode.NOTICE, ActionCode.READ)
            );

            String json = converter.convertToDatabaseColumn(original);
            List<PermissionAssignment> restored = converter.convertToEntityAttribute(json);

            assertThat(restored).hasSize(original.size());
            for (int i = 0; i < original.size(); i++) {
                assertThat(restored.get(i).getFeature()).isEqualTo(original.get(i).getFeature());
                assertThat(restored.get(i).getAction()).isEqualTo(original.get(i).getAction());
            }
        }
    }
}
