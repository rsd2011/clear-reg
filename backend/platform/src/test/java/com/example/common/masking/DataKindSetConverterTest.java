package com.example.common.masking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DataKindSetConverter")
class DataKindSetConverterTest {

    private final DataKindSetConverter converter = new DataKindSetConverter();

    @Nested
    @DisplayName("convertToDatabaseColumn")
    class ConvertToDatabase {

        @Test
        @DisplayName("null Set → '[]' 반환")
        void nullSetReturnsEmptyArray() {
            String result = converter.convertToDatabaseColumn(null);
            assertThat(result).isEqualTo("[]");
        }

        @Test
        @DisplayName("빈 Set → '[]' 반환")
        void emptySetReturnsEmptyArray() {
            String result = converter.convertToDatabaseColumn(Collections.emptySet());
            assertThat(result).isEqualTo("[]");
        }

        @Test
        @DisplayName("단일 요소 Set → JSON 배열")
        void singleElementSet() {
            Set<DataKind> set = Set.of(DataKind.SSN);
            String result = converter.convertToDatabaseColumn(set);
            assertThat(result).isEqualTo("[\"SSN\"]");
        }

        @Test
        @DisplayName("복수 요소 Set → JSON 배열")
        void multipleElementsSet() {
            Set<DataKind> set = EnumSet.of(DataKind.SSN, DataKind.PHONE);
            String result = converter.convertToDatabaseColumn(set);
            assertThat(result).contains("\"SSN\"", "\"PHONE\"");
            assertThat(result).startsWith("[").endsWith("]");
        }
    }

    @Nested
    @DisplayName("convertToEntityAttribute")
    class ConvertToEntity {

        @Test
        @DisplayName("null → 빈 Set 반환")
        void nullReturnsEmptySet() {
            Set<DataKind> result = converter.convertToEntityAttribute(null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("빈 문자열 → 빈 Set 반환")
        void emptyStringReturnsEmptySet() {
            Set<DataKind> result = converter.convertToEntityAttribute("");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("공백 문자열 → 빈 Set 반환")
        void blankStringReturnsEmptySet() {
            Set<DataKind> result = converter.convertToEntityAttribute("   ");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("'[]' → 빈 Set 반환")
        void emptyArrayReturnsEmptySet() {
            Set<DataKind> result = converter.convertToEntityAttribute("[]");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("'  []  ' (공백 포함) → 빈 Set 반환")
        void emptyArrayWithSpacesReturnsEmptySet() {
            Set<DataKind> result = converter.convertToEntityAttribute("  []  ");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("단일 요소 JSON 배열 → Set<DataKind>")
        void singleElementJsonArray() {
            Set<DataKind> result = converter.convertToEntityAttribute("[\"SSN\"]");
            assertThat(result).containsExactly(DataKind.SSN);
        }

        @Test
        @DisplayName("복수 요소 JSON 배열 → Set<DataKind>")
        void multipleElementsJsonArray() {
            Set<DataKind> result = converter.convertToEntityAttribute("[\"SSN\", \"PHONE\", \"EMAIL\"]");
            assertThat(result).containsExactlyInAnyOrder(DataKind.SSN, DataKind.PHONE, DataKind.EMAIL);
        }

        @Test
        @DisplayName("잘못된 JSON → IllegalStateException")
        void invalidJsonThrowsException() {
            assertThatThrownBy(() -> converter.convertToEntityAttribute("invalid"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to deserialize");
        }

        @Test
        @DisplayName("알 수 없는 DataKind 이름 → DEFAULT로 변환")
        void unknownDataKindNameBecomesDefault() {
            Set<DataKind> result = converter.convertToEntityAttribute("[\"UNKNOWN_KIND\"]");
            assertThat(result).containsExactly(DataKind.DEFAULT);
        }
    }

    @Test
    @DisplayName("왕복 변환 - convertTo → convertFrom")
    void roundTripConversion() {
        Set<DataKind> original = EnumSet.of(DataKind.SSN, DataKind.ACCOUNT_NO, DataKind.CARD_NO);
        String json = converter.convertToDatabaseColumn(original);
        Set<DataKind> restored = converter.convertToEntityAttribute(json);
        assertThat(restored).containsExactlyInAnyOrderElementsOf(original);
    }
}
