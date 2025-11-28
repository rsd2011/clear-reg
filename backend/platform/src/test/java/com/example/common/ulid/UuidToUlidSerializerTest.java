package com.example.common.ulid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.github.f4b6a3.ulid.UlidCreator;

@DisplayName("UuidToUlidSerializer 테스트")
class UuidToUlidSerializerTest {

    private final UuidToUlidSerializer serializer = new UuidToUlidSerializer();

    @Test
    @DisplayName("UUID를 ULID 문자열로 직렬화한다")
    void shouldSerializeUuidAsUlid() throws IOException {
        // Given
        UUID uuid = UlidCreator.getMonotonicUlid().toUuid();
        JsonGenerator gen = mock(JsonGenerator.class);
        SerializerProvider provider = mock(SerializerProvider.class);

        // When
        serializer.serialize(uuid, gen, provider);

        // Then
        String expectedUlid = UlidUtils.toUlidString(uuid);
        verify(gen).writeString(expectedUlid);
    }

    @Test
    @DisplayName("null UUID를 null로 직렬화한다")
    void shouldSerializeNullAsNull() throws IOException {
        // Given
        JsonGenerator gen = mock(JsonGenerator.class);
        SerializerProvider provider = mock(SerializerProvider.class);

        // When
        serializer.serialize(null, gen, provider);

        // Then
        verify(gen).writeNull();
    }

    @Test
    @DisplayName("handledType은 UUID.class를 반환한다")
    void shouldReturnUuidClass() {
        // When & Then
        assertThat(serializer.handledType()).isEqualTo(UUID.class);
    }
}
