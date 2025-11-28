package com.example.common.ulid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.github.f4b6a3.ulid.UlidCreator;

@DisplayName("UlidJacksonModule 테스트")
class UlidJacksonModuleTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new UlidJacksonModule());
    }

    static class TestDto {
        public UUID id;
        public String name;

        public TestDto() {}

        public TestDto(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @Nested
    @DisplayName("직렬화 (UUID → ULID 문자열)")
    class Serialization {

        @Test
        @DisplayName("UUID를 ULID 형식 문자열로 직렬화한다")
        void shouldSerializeUuidAsUlid() throws JsonProcessingException {
            // Given
            UUID uuid = UlidCreator.getMonotonicUlid().toUuid();
            TestDto dto = new TestDto(uuid, "test");

            // When
            String json = objectMapper.writeValueAsString(dto);

            // Then
            String expectedUlid = UlidUtils.toUlidString(uuid);
            assertThat(json).contains("\"id\":\"" + expectedUlid + "\"");
            assertThat(json).doesNotContain("-"); // UUID 형식의 하이픈이 없어야 함
        }

        @Test
        @DisplayName("null UUID는 null로 직렬화한다")
        void shouldSerializeNullAsNull() throws JsonProcessingException {
            // Given
            TestDto dto = new TestDto(null, "test");

            // When
            String json = objectMapper.writeValueAsString(dto);

            // Then
            assertThat(json).contains("\"id\":null");
        }

        @Test
        @DisplayName("직렬화된 ULID는 26자이다")
        void shouldSerializeAs26Characters() throws JsonProcessingException {
            // Given
            UUID uuid = UlidCreator.getMonotonicUlid().toUuid();
            TestDto dto = new TestDto(uuid, "test");

            // When
            String json = objectMapper.writeValueAsString(dto);
            TestDto parsed = objectMapper.readValue(json, TestDto.class);
            String ulidString = UlidUtils.toUlidString(parsed.id);

            // Then
            assertThat(ulidString).hasSize(26);
        }
    }

    @Nested
    @DisplayName("역직렬화 (문자열 → UUID)")
    class Deserialization {

        @Test
        @DisplayName("ULID 형식 문자열을 UUID로 역직렬화한다")
        void shouldDeserializeUlidToUuid() throws JsonProcessingException {
            // Given
            UUID original = UlidCreator.getMonotonicUlid().toUuid();
            String ulidString = UlidUtils.toUlidString(original);
            String json = "{\"id\":\"" + ulidString + "\",\"name\":\"test\"}";

            // When
            TestDto dto = objectMapper.readValue(json, TestDto.class);

            // Then
            assertThat(dto.id).isEqualTo(original);
        }

        @Test
        @DisplayName("UUID 형식 문자열을 UUID로 역직렬화한다 (하위 호환)")
        void shouldDeserializeUuidStringToUuid() throws JsonProcessingException {
            // Given
            UUID original = UUID.randomUUID();
            String json = "{\"id\":\"" + original + "\",\"name\":\"test\"}";

            // When
            TestDto dto = objectMapper.readValue(json, TestDto.class);

            // Then
            assertThat(dto.id).isEqualTo(original);
        }

        @Test
        @DisplayName("소문자 ULID도 역직렬화한다")
        void shouldDeserializeLowercaseUlid() throws JsonProcessingException {
            // Given
            UUID original = UlidCreator.getMonotonicUlid().toUuid();
            String ulidString = UlidUtils.toUlidString(original).toLowerCase();
            String json = "{\"id\":\"" + ulidString + "\",\"name\":\"test\"}";

            // When
            TestDto dto = objectMapper.readValue(json, TestDto.class);

            // Then
            assertThat(dto.id).isEqualTo(original);
        }

        @Test
        @DisplayName("null 값을 역직렬화한다")
        void shouldDeserializeNull() throws JsonProcessingException {
            // Given
            String json = "{\"id\":null,\"name\":\"test\"}";

            // When
            TestDto dto = objectMapper.readValue(json, TestDto.class);

            // Then
            assertThat(dto.id).isNull();
        }

        @Test
        @DisplayName("빈 문자열은 null로 역직렬화한다")
        void shouldDeserializeEmptyStringAsNull() throws JsonProcessingException {
            // Given
            String json = "{\"id\":\"\",\"name\":\"test\"}";

            // When
            TestDto dto = objectMapper.readValue(json, TestDto.class);

            // Then
            assertThat(dto.id).isNull();
        }

        @Test
        @DisplayName("공백 문자열은 null로 역직렬화한다")
        void shouldDeserializeBlankStringAsNull() throws JsonProcessingException {
            // Given
            String json = "{\"id\":\"   \",\"name\":\"test\"}";

            // When
            TestDto dto = objectMapper.readValue(json, TestDto.class);

            // Then
            assertThat(dto.id).isNull();
        }

        @Test
        @DisplayName("잘못된 형식은 예외를 발생시킨다")
        void shouldThrowExceptionForInvalidFormat() {
            // Given
            String json = "{\"id\":\"invalid-format\",\"name\":\"test\"}";

            // When & Then
            assertThatThrownBy(() -> objectMapper.readValue(json, TestDto.class))
                .isInstanceOf(InvalidFormatException.class)
                .hasMessageContaining("Invalid ID format");
        }

        @Test
        @DisplayName("숫자 타입은 예외를 발생시킨다")
        void shouldThrowExceptionForNumberType() {
            // Given
            String json = "{\"id\":12345,\"name\":\"test\"}";

            // When & Then
            assertThatThrownBy(() -> objectMapper.readValue(json, TestDto.class))
                .isInstanceOf(InvalidFormatException.class)
                .hasMessageContaining("Expected string value");
        }
    }

    @Nested
    @DisplayName("왕복 변환 (Round-trip)")
    class RoundTrip {

        @Test
        @DisplayName("직렬화 후 역직렬화하면 원본과 같다")
        void shouldPreserveValueAfterRoundTrip() throws JsonProcessingException {
            // Given
            UUID original = UlidCreator.getMonotonicUlid().toUuid();
            TestDto dto = new TestDto(original, "test");

            // When
            String json = objectMapper.writeValueAsString(dto);
            TestDto restored = objectMapper.readValue(json, TestDto.class);

            // Then
            assertThat(restored.id).isEqualTo(original);
            assertThat(restored.name).isEqualTo("test");
        }

        @Test
        @DisplayName("여러 번 왕복해도 값이 유지된다")
        void shouldPreserveValueAfterMultipleRoundTrips() throws JsonProcessingException {
            // Given
            UUID original = UlidCreator.getMonotonicUlid().toUuid();
            TestDto dto = new TestDto(original, "test");

            // When
            for (int i = 0; i < 5; i++) {
                String json = objectMapper.writeValueAsString(dto);
                dto = objectMapper.readValue(json, TestDto.class);
            }

            // Then
            assertThat(dto.id).isEqualTo(original);
        }
    }
}
