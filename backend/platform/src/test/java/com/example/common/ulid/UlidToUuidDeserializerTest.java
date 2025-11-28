package com.example.common.ulid;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UlidToUuidDeserializer 테스트")
class UlidToUuidDeserializerTest {

    private final UlidToUuidDeserializer deserializer = new UlidToUuidDeserializer();

    @Test
    @DisplayName("handledType은 UUID.class를 반환한다")
    void shouldReturnUuidClass() {
        // When & Then
        assertThat(deserializer.handledType()).isEqualTo(UUID.class);
    }

    @Test
    @DisplayName("getNullValue는 null을 반환한다")
    void shouldReturnNullForNullValue() {
        // When & Then
        assertThat(deserializer.getNullValue(null)).isNull();
    }
}
