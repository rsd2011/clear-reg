package com.example.common.ulid;

import java.io.IOException;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

/**
 * ULID 또는 UUID 문자열을 UUID로 역직렬화하는 Jackson Deserializer.
 *
 * <p>JSON 요청에서 ULID(26자) 또는 UUID(36자) 형식 문자열을 UUID로 변환합니다.
 * 하위 호환성을 위해 두 형식 모두 지원합니다.</p>
 *
 * <h3>입력 예시</h3>
 * <pre>{@code
 * // ULID 형식
 * { "id": "01ARZ3NDEKTSV4RRFFQ69G5FAV" }
 *
 * // UUID 형식 (하위 호환)
 * { "id": "550e8400-e29b-41d4-a716-446655440000" }
 * }</pre>
 *
 * @see UuidToUlidSerializer
 * @see UlidJacksonModule
 */
public class UlidToUuidDeserializer extends JsonDeserializer<UUID> {

    @Override
    public UUID deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }

        if (p.currentToken() != JsonToken.VALUE_STRING) {
            throw InvalidFormatException.from(p,
                    "Expected string value for UUID field",
                    p.getText(),
                    UUID.class);
        }

        String value = p.getText();
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return UlidUtils.fromString(value);
        } catch (IllegalArgumentException e) {
            throw InvalidFormatException.from(p,
                    "Invalid ID format: '" + value + "'. " +
                    "Expected ULID (26 chars, e.g., 01ARZ3NDEKTSV4RRFFQ69G5FAV) " +
                    "or UUID (36 chars, e.g., 550e8400-e29b-41d4-a716-446655440000).",
                    value,
                    UUID.class);
        }
    }

    @Override
    public Class<?> handledType() {
        return UUID.class;
    }

    @Override
    public UUID getNullValue(DeserializationContext ctxt) {
        return null;
    }
}
