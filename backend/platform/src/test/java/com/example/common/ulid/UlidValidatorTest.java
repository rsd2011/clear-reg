package com.example.common.ulid;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.f4b6a3.ulid.UlidCreator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@DisplayName("UlidValidator 테스트")
class UlidValidatorTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("기본 모드 (ULID 및 UUID 허용)")
    class DefaultMode {

        static class TestDto {
            @ValidUlid
            String id;

            TestDto(String id) {
                this.id = id;
            }
        }

        @Test
        @DisplayName("유효한 ULID 형식을 통과한다")
        void shouldPassValidUlid() {
            // Given
            String ulid = UlidUtils.toUlidString(UlidCreator.getMonotonicUlid().toUuid());
            TestDto dto = new TestDto(ulid);

            // When
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("유효한 UUID 형식을 통과한다")
        void shouldPassValidUuid() {
            // Given
            String uuid = UUID.randomUUID().toString();
            TestDto dto = new TestDto(uuid);

            // When
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

            // Then
            assertThat(violations).isEmpty();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t"})
        @DisplayName("null 또는 빈 문자열을 통과한다 (별도 @NotNull 검증)")
        void shouldPassNullOrEmpty(String input) {
            // Given
            TestDto dto = new TestDto(input);

            // When
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

            // Then
            assertThat(violations).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"invalid", "12345", "not-a-valid-id"})
        @DisplayName("잘못된 형식은 실패한다")
        void shouldFailInvalidFormat(String input) {
            // Given
            TestDto dto = new TestDto(input);

            // When
            Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

            // Then
            assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Invalid ID format. Expected ULID (26 chars) or UUID (36 chars).");
        }
    }

    @Nested
    @DisplayName("ULID 전용 모드 (ulidOnly = true)")
    class UlidOnlyMode {

        static class UlidOnlyDto {
            @ValidUlid(ulidOnly = true)
            String id;

            UlidOnlyDto(String id) {
                this.id = id;
            }
        }

        @Test
        @DisplayName("유효한 ULID 형식을 통과한다")
        void shouldPassValidUlid() {
            // Given
            String ulid = UlidUtils.toUlidString(UlidCreator.getMonotonicUlid().toUuid());
            UlidOnlyDto dto = new UlidOnlyDto(ulid);

            // When
            Set<ConstraintViolation<UlidOnlyDto>> violations = validator.validate(dto);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("UUID 형식은 실패한다")
        void shouldFailUuidFormat() {
            // Given
            String uuid = UUID.randomUUID().toString();
            UlidOnlyDto dto = new UlidOnlyDto(uuid);

            // When
            Set<ConstraintViolation<UlidOnlyDto>> violations = validator.validate(dto);

            // Then
            assertThat(violations).hasSize(1);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 또는 빈 문자열을 통과한다")
        void shouldPassNullOrEmpty(String input) {
            // Given
            UlidOnlyDto dto = new UlidOnlyDto(input);

            // When
            Set<ConstraintViolation<UlidOnlyDto>> violations = validator.validate(dto);

            // Then
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("커스텀 메시지")
    class CustomMessage {

        static class CustomMessageDto {
            @ValidUlid(message = "올바른 ID 형식이 아닙니다")
            String id;

            CustomMessageDto(String id) {
                this.id = id;
            }
        }

        @Test
        @DisplayName("커스텀 에러 메시지를 사용할 수 있다")
        void shouldUseCustomMessage() {
            // Given
            CustomMessageDto dto = new CustomMessageDto("invalid");

            // When
            Set<ConstraintViolation<CustomMessageDto>> violations = validator.validate(dto);

            // Then
            assertThat(violations)
                .hasSize(1)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("올바른 ID 형식이 아닙니다");
        }
    }
}
