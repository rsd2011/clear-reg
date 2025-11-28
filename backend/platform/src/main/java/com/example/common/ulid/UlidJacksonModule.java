package com.example.common.ulid;

import java.util.UUID;

import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * ULID 형식의 UUID 직렬화/역직렬화를 위한 Jackson Module.
 *
 * <p>이 모듈을 등록하면 모든 UUID 필드가 JSON에서 ULID 형식으로 직렬화되고,
 * ULID 또는 UUID 형식 문자열이 UUID로 역직렬화됩니다.</p>
 *
 * <h3>등록 방법</h3>
 *
 * <h4>Spring Boot 자동 설정 (권장)</h4>
 * <pre>{@code
 * @Configuration
 * public class JacksonConfig {
 *     @Bean
 *     public UlidJacksonModule ulidJacksonModule() {
 *         return new UlidJacksonModule();
 *     }
 * }
 * }</pre>
 *
 * <h4>ObjectMapper 직접 등록</h4>
 * <pre>{@code
 * ObjectMapper mapper = new ObjectMapper();
 * mapper.registerModule(new UlidJacksonModule());
 * }</pre>
 *
 * <h3>변환 예시</h3>
 * <pre>{@code
 * // 직렬화 (UUID → ULID 문자열)
 * { "id": "01ARZ3NDEKTSV4RRFFQ69G5FAV" }
 *
 * // 역직렬화 (ULID 또는 UUID 문자열 → UUID)
 * // 입력: "01ARZ3NDEKTSV4RRFFQ69G5FAV" 또는 "550e8400-e29b-41d4-a716-446655440000"
 * // 결과: UUID 객체
 * }</pre>
 *
 * @see UuidToUlidSerializer
 * @see UlidToUuidDeserializer
 */
public class UlidJacksonModule extends SimpleModule {

    private static final String MODULE_NAME = "UlidJacksonModule";

    public UlidJacksonModule() {
        super(MODULE_NAME);
        addSerializer(UUID.class, new UuidToUlidSerializer());
        addDeserializer(UUID.class, new UlidToUuidDeserializer());
    }
}
