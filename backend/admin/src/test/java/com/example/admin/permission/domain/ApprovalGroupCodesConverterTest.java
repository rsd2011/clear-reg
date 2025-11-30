package com.example.admin.permission.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ApprovalGroupCodesConverter 테스트")
class ApprovalGroupCodesConverterTest {

    private ApprovalGroupCodesConverter converter;

    @BeforeEach
    void setUp() {
        converter = new ApprovalGroupCodesConverter();
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
        @DisplayName("Given 그룹 코드 목록 When 변환 Then JSON 배열로 직렬화")
        void groupCodesToJson() {
            List<String> codes = List.of("GRP1", "GRP2", "GRP3");

            String result = converter.convertToDatabaseColumn(codes);

            assertThat(result).isEqualTo("[\"GRP1\",\"GRP2\",\"GRP3\"]");
        }

        @Test
        @DisplayName("Given 단일 그룹 코드 When 변환 Then 단일 요소 JSON 배열")
        void singleCodeToJson() {
            List<String> codes = List.of("SINGLE_GROUP");

            String result = converter.convertToDatabaseColumn(codes);

            assertThat(result).isEqualTo("[\"SINGLE_GROUP\"]");
        }
    }

    @Nested
    @DisplayName("convertToEntityAttribute 메서드")
    class ConvertToEntityAttributeTests {

        @Test
        @DisplayName("Given null 문자열 When 변환 Then 빈 리스트 반환")
        void nullStringReturnsEmptyList() {
            List<String> result = converter.convertToEntityAttribute(null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given 빈 문자열 When 변환 Then 빈 리스트 반환")
        void blankStringReturnsEmptyList() {
            List<String> result = converter.convertToEntityAttribute("   ");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given 빈 JSON 배열 When 변환 Then 빈 리스트 반환")
        void emptyArrayReturnsEmptyList() {
            List<String> result = converter.convertToEntityAttribute("[]");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given 유효한 JSON 배열 When 변환 Then 그룹 코드 목록 반환")
        void validJsonToGroupCodes() {
            String json = "[\"GRP1\",\"GRP2\",\"GRP3\"]";

            List<String> result = converter.convertToEntityAttribute(json);

            assertThat(result).containsExactly("GRP1", "GRP2", "GRP3");
        }

        @Test
        @DisplayName("Given 단일 요소 JSON 배열 When 변환 Then 단일 요소 리스트 반환")
        void singleElementJsonToList() {
            String json = "[\"ONLY_ONE\"]";

            List<String> result = converter.convertToEntityAttribute(json);

            assertThat(result).containsExactly("ONLY_ONE");
        }

        @Test
        @DisplayName("Given 잘못된 JSON When 변환 Then IllegalArgumentException 발생")
        void invalidJsonThrows() {
            String invalidJson = "not a valid json";

            assertThatThrownBy(() -> converter.convertToEntityAttribute(invalidJson))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Failed to convert JSON to approval group codes");
        }

        @Test
        @DisplayName("Given JSON 객체 When 변환 Then IllegalArgumentException 발생")
        void jsonObjectThrows() {
            String jsonObject = "{\"key\":\"value\"}";

            assertThatThrownBy(() -> converter.convertToEntityAttribute(jsonObject))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Failed to convert JSON to approval group codes");
        }
    }

    @Nested
    @DisplayName("왕복 변환 테스트")
    class RoundTripTests {

        @Test
        @DisplayName("Given 그룹 코드 목록 When DB 저장 후 조회 Then 원본과 동일")
        void roundTripPreservesData() {
            List<String> original = List.of("ADMIN", "MANAGER", "USER");

            String json = converter.convertToDatabaseColumn(original);
            List<String> restored = converter.convertToEntityAttribute(json);

            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("Given 특수 문자 포함 그룹 코드 When 변환 Then 올바르게 처리됨")
        void specialCharactersPreserved() {
            List<String> original = List.of("GRP-001", "GRP_002", "GRP.003");

            String json = converter.convertToDatabaseColumn(original);
            List<String> restored = converter.convertToEntityAttribute(json);

            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("Given 한글 그룹 코드 When 변환 Then 올바르게 처리됨")
        void koreanCharactersPreserved() {
            List<String> original = List.of("관리자그룹", "사용자그룹");

            String json = converter.convertToDatabaseColumn(original);
            List<String> restored = converter.convertToEntityAttribute(json);

            assertThat(restored).isEqualTo(original);
        }
    }
}
