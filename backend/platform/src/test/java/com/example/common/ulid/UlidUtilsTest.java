package com.example.common.ulid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.f4b6a3.ulid.UlidCreator;

@DisplayName("UlidUtils 테스트")
class UlidUtilsTest {

    @Nested
    @DisplayName("fromString 메서드")
    class FromString {

        @Test
        @DisplayName("ULID 문자열을 UUID로 변환한다")
        void shouldConvertUlidToUuid() {
            // Given
            UUID original = UlidCreator.getMonotonicUlid().toUuid();
            String ulidString = UlidUtils.toUlidString(original);

            // When
            UUID result = UlidUtils.fromString(ulidString);

            // Then
            assertThat(result).isEqualTo(original);
        }

        @Test
        @DisplayName("소문자 ULID 문자열도 변환한다")
        void shouldConvertLowercaseUlid() {
            // Given
            UUID original = UlidCreator.getMonotonicUlid().toUuid();
            String ulidString = UlidUtils.toUlidString(original).toLowerCase();

            // When
            UUID result = UlidUtils.fromString(ulidString);

            // Then
            assertThat(result).isEqualTo(original);
        }

        @Test
        @DisplayName("UUID 문자열을 UUID로 변환한다")
        void shouldConvertUuidStringToUuid() {
            // Given
            UUID original = UUID.randomUUID();
            String uuidString = original.toString();

            // When
            UUID result = UlidUtils.fromString(uuidString);

            // Then
            assertThat(result).isEqualTo(original);
        }

        @Test
        @DisplayName("대문자 UUID 문자열도 변환한다")
        void shouldConvertUppercaseUuid() {
            // Given
            UUID original = UUID.randomUUID();
            String uuidString = original.toString().toUpperCase();

            // When
            UUID result = UlidUtils.fromString(uuidString);

            // Then
            assertThat(result).isEqualTo(original);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t", "\n"})
        @DisplayName("null 또는 빈 문자열은 null을 반환한다")
        void shouldReturnNullForNullOrEmpty(String input) {
            // When
            UUID result = UlidUtils.fromString(input);

            // Then
            assertThat(result).isNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "invalid",
            "12345",
            "not-a-valid-ulid-or-uuid",
            "01ARZ3NDEKTSV4RRFFQ69G5FA",   // 25자 (1자 부족)
            "01ARZ3NDEKTSV4RRFFQ69G5FAVX", // 27자 (1자 초과)
            "550e8400-e29b-41d4-a716-44665544000",  // 35자 (1자 부족)
            "550e8400-e29b-41d4-a716-4466554400000" // 37자 (1자 초과)
        })
        @DisplayName("잘못된 형식은 예외를 발생시킨다")
        void shouldThrowExceptionForInvalidFormat(String input) {
            // When & Then
            assertThatThrownBy(() -> UlidUtils.fromString(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid ID format");
        }

        @Test
        @DisplayName("26자이지만 잘못된 ULID 형식은 예외를 발생시킨다")
        void shouldThrowExceptionForInvalidUlidFormat() {
            // Given - 26자이지만 유효하지 않은 ULID
            // ULID 첫 번째 문자는 0-7만 허용 (timestamp overflow 방지)
            // 8ZZZZZZZZZZZZZZZZZZZZZZZZZ는 첫 문자가 8이라 무효
            String invalidUlid = "8ZZZZZZZZZZZZZZZZZZZZZZZZZ";

            // When & Then
            assertThatThrownBy(() -> UlidUtils.fromString(invalidUlid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid ULID format");
        }

        @Test
        @DisplayName("36자이지만 잘못된 UUID 형식은 예외를 발생시킨다")
        void shouldThrowExceptionForInvalidUuidFormat() {
            // Given - 36자이지만 유효하지 않은 UUID 형식
            String invalidUuid = "550e8400-e29b-41d4-a716-GGGGGGGGGGGG";

            // When & Then
            assertThatThrownBy(() -> UlidUtils.fromString(invalidUuid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid UUID format");
        }

        @Test
        @DisplayName("앞뒤 공백이 있는 문자열도 처리한다")
        void shouldTrimWhitespace() {
            // Given
            UUID original = UlidCreator.getMonotonicUlid().toUuid();
            String ulidWithSpaces = "  " + UlidUtils.toUlidString(original) + "  ";

            // When
            UUID result = UlidUtils.fromString(ulidWithSpaces);

            // Then
            assertThat(result).isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("toUlidString 메서드")
    class ToUlidString {

        @Test
        @DisplayName("UUID를 ULID 문자열로 변환한다")
        void shouldConvertUuidToUlidString() {
            // Given
            UUID uuid = UlidCreator.getMonotonicUlid().toUuid();

            // When
            String result = UlidUtils.toUlidString(uuid);

            // Then
            assertThat(result)
                .hasSize(26)
                .matches("[0-9A-Z]+");
        }

        @Test
        @DisplayName("null UUID는 null을 반환한다")
        void shouldReturnNullForNullUuid() {
            // When
            String result = UlidUtils.toUlidString(null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("변환 후 다시 UUID로 복원 가능하다")
        void shouldBeReversible() {
            // Given
            UUID original = UlidCreator.getMonotonicUlid().toUuid();

            // When
            String ulidString = UlidUtils.toUlidString(original);
            UUID restored = UlidUtils.fromString(ulidString);

            // Then
            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("ULID 문자열 → UUID → ULID 문자열 변환 시 동일한 값을 유지한다")
        void shouldPreserveUlidStringAfterRoundTrip() {
            // Given - ULID 원본 문자열
            var ulid = UlidCreator.getMonotonicUlid();
            String originalUlidString = ulid.toString();

            // When - ULID → UUID → ULID 변환
            UUID uuid = ulid.toUuid();
            String restoredUlidString = UlidUtils.toUlidString(uuid);

            // Then - 원본 ULID 문자열과 동일해야 함
            assertThat(restoredUlidString).isEqualTo(originalUlidString);
        }

        @Test
        @DisplayName("여러 번 왕복 변환해도 ULID 문자열이 동일하다")
        void shouldPreserveUlidStringAfterMultipleRoundTrips() {
            // Given
            var ulid = UlidCreator.getMonotonicUlid();
            String originalUlidString = ulid.toString();
            UUID uuid = ulid.toUuid();

            // When - 여러 번 왕복 변환
            for (int i = 0; i < 5; i++) {
                String ulidString = UlidUtils.toUlidString(uuid);
                uuid = UlidUtils.fromString(ulidString);
            }
            String finalUlidString = UlidUtils.toUlidString(uuid);

            // Then
            assertThat(finalUlidString).isEqualTo(originalUlidString);
        }
    }

    @Nested
    @DisplayName("정렬 순서 보존")
    class SortOrderPreservation {

        @Test
        @DisplayName("ULID에서 변환된 UUID는 생성 순서대로 정렬된다")
        void shouldPreserveSortOrderWhenConvertedToUuid() throws InterruptedException {
            // Given - 시간 간격을 두고 ULID 생성
            List<UUID> originalOrder = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                originalOrder.add(UlidCreator.getMonotonicUlid().toUuid());
                Thread.sleep(5); // 5ms 간격
            }

            // When - UUID 정렬
            List<UUID> sortedOrder = new ArrayList<>(originalOrder);
            Collections.sort(sortedOrder);

            // Then - 정렬 후에도 생성 순서가 유지되어야 함
            assertThat(sortedOrder).containsExactlyElementsOf(originalOrder);
        }

        @Test
        @DisplayName("ULID 문자열과 UUID의 정렬 순서가 일치한다")
        void shouldHaveConsistentSortOrderBetweenUlidAndUuid() throws InterruptedException {
            // Given
            List<String> ulidStrings = new ArrayList<>();
            List<UUID> uuids = new ArrayList<>();

            for (int i = 0; i < 5; i++) {
                var ulid = UlidCreator.getMonotonicUlid();
                ulidStrings.add(ulid.toString());
                uuids.add(ulid.toUuid());
                Thread.sleep(5);
            }

            // When - 각각 정렬
            List<String> sortedUlids = new ArrayList<>(ulidStrings);
            Collections.sort(sortedUlids);

            List<UUID> sortedUuids = new ArrayList<>(uuids);
            Collections.sort(sortedUuids);

            // Then - 정렬 결과의 인덱스가 동일해야 함
            for (int i = 0; i < 5; i++) {
                int ulidIndex = ulidStrings.indexOf(sortedUlids.get(i));
                int uuidIndex = uuids.indexOf(sortedUuids.get(i));
                assertThat(ulidIndex)
                    .as("정렬 후 %d번째 요소의 원본 인덱스가 일치해야 함", i)
                    .isEqualTo(uuidIndex);
            }
        }

        @Test
        @DisplayName("동일 밀리초 내에서도 Monotonic ULID는 순서를 보장한다")
        void shouldPreserveOrderWithinSameMillisecond() {
            // Given - 동일 밀리초 내에서 여러 ULID 생성 (sleep 없음)
            List<UUID> originalOrder = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                originalOrder.add(UlidCreator.getMonotonicUlid().toUuid());
            }

            // When
            List<UUID> sortedOrder = new ArrayList<>(originalOrder);
            Collections.sort(sortedOrder);

            // Then
            assertThat(sortedOrder).containsExactlyElementsOf(originalOrder);
        }
    }

    @Nested
    @DisplayName("isValidUlid 메서드")
    class IsValidUlid {

        @Test
        @DisplayName("유효한 ULID 형식을 인식한다")
        void shouldRecognizeValidUlid() {
            // Given
            String ulid = UlidUtils.toUlidString(UlidCreator.getMonotonicUlid().toUuid());

            // When & Then
            assertThat(UlidUtils.isValidUlid(ulid)).isTrue();
        }

        @Test
        @DisplayName("소문자 ULID도 유효하다")
        void shouldAcceptLowercaseUlid() {
            // Given
            String ulid = UlidUtils.toUlidString(UlidCreator.getMonotonicUlid().toUuid()).toLowerCase();

            // When & Then
            assertThat(UlidUtils.isValidUlid(ulid)).isTrue();
        }

        @Test
        @DisplayName("UUID 형식은 유효하지 않다")
        void shouldRejectUuidFormat() {
            // Given
            String uuid = UUID.randomUUID().toString();

            // When & Then
            assertThat(UlidUtils.isValidUlid(uuid)).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 또는 빈 문자열은 유효하지 않다")
        void shouldRejectNullOrEmpty(String input) {
            // When & Then
            assertThat(UlidUtils.isValidUlid(input)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"ILOU", "01ARZ3NDEKTSILOU9G5FAV"})
        @DisplayName("Crockford Base32에서 제외된 문자(I,L,O,U)가 포함되면 유효하지 않다")
        void shouldRejectExcludedCharacters(String input) {
            // When & Then
            assertThat(UlidUtils.isValidUlid(input)).isFalse();
        }
    }

    @Nested
    @DisplayName("isValidUuid 메서드")
    class IsValidUuid {

        @Test
        @DisplayName("유효한 UUID 형식을 인식한다")
        void shouldRecognizeValidUuid() {
            // Given
            String uuid = UUID.randomUUID().toString();

            // When & Then
            assertThat(UlidUtils.isValidUuid(uuid)).isTrue();
        }

        @Test
        @DisplayName("대문자 UUID도 유효하다")
        void shouldAcceptUppercaseUuid() {
            // Given
            String uuid = UUID.randomUUID().toString().toUpperCase();

            // When & Then
            assertThat(UlidUtils.isValidUuid(uuid)).isTrue();
        }

        @Test
        @DisplayName("ULID 형식은 유효하지 않다")
        void shouldRejectUlidFormat() {
            // Given
            String ulid = UlidUtils.toUlidString(UlidCreator.getMonotonicUlid().toUuid());

            // When & Then
            assertThat(UlidUtils.isValidUuid(ulid)).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 또는 빈 문자열은 유효하지 않다")
        void shouldRejectNullOrEmpty(String input) {
            // When & Then
            assertThat(UlidUtils.isValidUuid(input)).isFalse();
        }
    }

    @Nested
    @DisplayName("isValidId 메서드")
    class IsValidId {

        @Test
        @DisplayName("ULID 형식을 유효하다고 인식한다")
        void shouldAcceptUlidFormat() {
            // Given
            String ulid = UlidUtils.toUlidString(UlidCreator.getMonotonicUlid().toUuid());

            // When & Then
            assertThat(UlidUtils.isValidId(ulid)).isTrue();
        }

        @Test
        @DisplayName("UUID 형식을 유효하다고 인식한다")
        void shouldAcceptUuidFormat() {
            // Given
            String uuid = UUID.randomUUID().toString();

            // When & Then
            assertThat(UlidUtils.isValidId(uuid)).isTrue();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 또는 빈 문자열은 유효하지 않다")
        void shouldRejectNullOrEmpty(String input) {
            // When & Then
            assertThat(UlidUtils.isValidId(input)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"invalid", "12345", "not-valid"})
        @DisplayName("ULID도 UUID도 아닌 형식은 유효하지 않다")
        void shouldRejectInvalidFormats(String input) {
            // When & Then
            assertThat(UlidUtils.isValidId(input)).isFalse();
        }
    }
}
