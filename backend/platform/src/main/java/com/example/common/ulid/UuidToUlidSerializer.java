package com.example.common.ulid;

import java.io.IOException;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * UUID를 ULID 문자열로 직렬화하는 Jackson Serializer.
 *
 * <p>JSON 응답에서 UUID 필드를 26자 ULID 형식으로 출력합니다.</p>
 *
 * <h3>출력 예시</h3>
 * <pre>{@code
 * // Java 객체
 * record Response(UUID id) {}
 *
 * // JSON 출력
 * { "id": "01ARZ3NDEKTSV4RRFFQ69G5FAV" }
 * }</pre>
 *
 * @see UlidToUuidDeserializer
 * @see UlidJacksonModule
 */
public class UuidToUlidSerializer extends JsonSerializer<UUID> {

    @Override
    public void serialize(UUID value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeString(UlidUtils.toUlidString(value));
        }
    }

    @Override
    public Class<UUID> handledType() {
        return UUID.class;
    }
}
