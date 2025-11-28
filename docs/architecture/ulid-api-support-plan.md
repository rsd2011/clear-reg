# ULID 문자열 API 지원 구현 계획

> **생성일**: 2025-01-28
> **목적**: API 계층에서 ULID 문자열 입출력 지원

---

## 1. 목표

API에서 ULID 문자열(26자)을 직접 사용할 수 있도록 지원:
- **입력**: ULID(26자) 또는 UUID(36자) → UUID로 변환
- **출력**: UUID → ULID 문자열(26자)로 직렬화

### 1.1 ULID의 외부 입력 이점

| 항목 | UUID | ULID | 이점 |
|------|------|------|------|
| 길이 | 36자 | 26자 | **28% 짧음** |
| URL 안전성 | 하이픈 포함 | 완전 URL-safe | 인코딩 불필요 |
| 혼동 문자 | 모든 hex | I,L,O,U 제외 | **입력 오류 감소** |
| 정렬 | 불가(v4) | 시간순 정렬 | 클라이언트 정렬 용이 |

---

## 2. 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                        API Layer                             │
├─────────────────────────────────────────────────────────────┤
│  @PathVariable    @RequestParam    @RequestBody (JSON)      │
│       ↓                 ↓                  ↓                │
│  ArgumentResolver   Converter      Jackson Module           │
│       ↓                 ↓                  ↓                │
│              ┌─────────────────────┐                        │
│              │     UlidUtils       │                        │
│              │  ULID ↔ UUID 변환   │                        │
│              └─────────────────────┘                        │
│                        ↓                                    │
├─────────────────────────────────────────────────────────────┤
│                   Service Layer (UUID)                       │
├─────────────────────────────────────────────────────────────┤
│                   Repository Layer (UUID)                    │
├─────────────────────────────────────────────────────────────┤
│                   Database (UUID column)                     │
└─────────────────────────────────────────────────────────────┘
```

**핵심**: Entity/Repository 변경 없이 API 계층에서만 ULID ↔ UUID 변환

---

## 3. 구현 파일 목록

### 3.1 Core Utility (platform 모듈)

```
backend/platform/src/main/java/com/example/common/ulid/
├── UlidUtils.java              # ULID ↔ UUID 변환 유틸
├── UlidValidator.java          # ULID 형식 검증
└── ValidUlid.java              # Bean Validation 어노테이션
```

### 3.2 Jackson Module (platform 모듈)

```
backend/platform/src/main/java/com/example/common/ulid/
├── UlidJacksonModule.java      # Jackson 모듈 등록
├── UuidToUlidSerializer.java   # UUID → ULID 문자열
└── UlidToUuidDeserializer.java # ULID/UUID 문자열 → UUID
```

### 3.3 Spring MVC (server 모듈)

```
backend/server/src/main/java/com/example/server/config/ulid/
├── UlidWebMvcConfig.java          # WebMvcConfigurer
├── UlidArgumentResolver.java      # @PathVariable, @RequestParam
├── StringToUuidConverterFactory.java  # @ModelAttribute, Form
└── UlidOpenApiConfig.java         # Swagger 문서화
```

### 3.4 Tests

```
backend/platform/src/test/java/com/example/common/ulid/
├── UlidUtilsTest.java
├── UlidValidatorTest.java
└── UlidJacksonModuleTest.java
```

---

## 4. 상세 구현

### 4.1 UlidUtils.java

```java
package com.example.common.ulid;

import com.github.f4b6a3.ulid.Ulid;
import java.util.UUID;

public final class UlidUtils {

    private static final int ULID_LENGTH = 26;
    private static final int UUID_LENGTH = 36;

    private UlidUtils() {}

    /**
     * ULID(26자) 또는 UUID(36자) 문자열을 UUID로 변환.
     * 하위 호환성을 위해 두 형식 모두 지원.
     */
    public static UUID fromString(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String trimmed = input.trim();

        if (trimmed.length() == ULID_LENGTH) {
            return Ulid.from(trimmed).toUuid();
        } else if (trimmed.length() == UUID_LENGTH) {
            return UUID.fromString(trimmed);
        }

        throw new IllegalArgumentException(
            "Invalid ID format. Expected ULID (26 chars) or UUID (36 chars): " + input);
    }

    /**
     * UUID를 ULID 문자열(26자)로 변환.
     */
    public static String toUlidString(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return Ulid.from(uuid).toString();
    }

    /**
     * ULID 형식인지 검증.
     */
    public static boolean isValidUlid(String input) {
        if (input == null || input.length() != ULID_LENGTH) {
            return false;
        }
        return input.matches("^[0-9A-HJKMNP-TV-Z]{26}$");
    }
}
```

### 4.2 UlidJacksonModule.java

```java
package com.example.common.ulid;

import com.fasterxml.jackson.databind.module.SimpleModule;
import java.util.UUID;

/**
 * UUID 필드를 ULID 문자열로 직렬화/역직렬화하는 Jackson 모듈.
 *
 * 동작:
 * - 직렬화: UUID → "01ARZ3NDEKTSV4RRFFQ69G5FAV" (26자)
 * - 역직렬화: "01ARZ3NDEKTSV4RRFFQ69G5FAV" 또는 UUID 형식 → UUID
 */
public class UlidJacksonModule extends SimpleModule {

    public UlidJacksonModule() {
        super("UlidJacksonModule");
        addSerializer(UUID.class, new UuidToUlidSerializer());
        addDeserializer(UUID.class, new UlidToUuidDeserializer());
    }
}
```

### 4.3 UuidToUlidSerializer.java

```java
package com.example.common.ulid;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.UUID;

public class UuidToUlidSerializer extends JsonSerializer<UUID> {

    @Override
    public void serialize(UUID value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeString(UlidUtils.toUlidString(value));
        }
    }
}
```

### 4.4 UlidToUuidDeserializer.java

```java
package com.example.common.ulid;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.UUID;

public class UlidToUuidDeserializer extends JsonDeserializer<UUID> {

    @Override
    public UUID deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        String value = p.getValueAsString();
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return UlidUtils.fromString(value);
        } catch (IllegalArgumentException e) {
            throw ctxt.weirdStringException(value, UUID.class, e.getMessage());
        }
    }
}
```

### 4.5 UlidArgumentResolver.java

```java
package com.example.server.config.ulid;

import com.example.common.ulid.UlidUtils;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

/**
 * @PathVariable, @RequestParam에서 ULID/UUID 문자열을 UUID로 변환.
 */
public class UlidArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(UUID.class)
            && (parameter.hasParameterAnnotation(PathVariable.class)
                || parameter.hasParameterAnnotation(RequestParam.class));
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        String paramName = getParameterName(parameter);
        String value = webRequest.getParameter(paramName);

        if (value == null) {
            // @PathVariable의 경우 URI template variable에서 가져옴
            value = (String) webRequest.getAttribute(
                "org.springframework.web.servlet.HandlerMapping.uriTemplateVariables",
                NativeWebRequest.SCOPE_REQUEST);
        }

        if (value == null || value.isBlank()) {
            return null;
        }

        return UlidUtils.fromString(value);
    }

    private String getParameterName(MethodParameter parameter) {
        PathVariable pathVariable = parameter.getParameterAnnotation(PathVariable.class);
        if (pathVariable != null && !pathVariable.value().isEmpty()) {
            return pathVariable.value();
        }

        RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
        if (requestParam != null && !requestParam.value().isEmpty()) {
            return requestParam.value();
        }

        return parameter.getParameterName();
    }
}
```

### 4.6 UlidWebMvcConfig.java

```java
package com.example.server.config.ulid;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class UlidWebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new UlidArgumentResolver());
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new StringToUuidConverterFactory());
    }
}
```

---

## 5. API 변경 사항

### Before (UUID)
```
GET /api/drafts/123e4567-e89b-12d3-a456-426614174000

Response:
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "title": "제목"
}
```

### After (ULID)
```
GET /api/drafts/01ARZ3NDEKTSV4RRFFQ69G5FAV

Response:
{
  "id": "01ARZ3NDEKTSV4RRFFQ69G5FAV",
  "title": "제목"
}

# 하위 호환: UUID도 계속 동작
GET /api/drafts/123e4567-e89b-12d3-a456-426614174000
```

---

## 6. 구현 순서

### Phase 1: Core (platform 모듈)
1. [ ] `UlidUtils.java` - 변환 유틸
2. [ ] `UlidValidator.java` + `@ValidUlid` - 검증
3. [ ] Jackson 모듈 (Serializer/Deserializer)
4. [ ] 단위 테스트

### Phase 2: Spring MVC (server 모듈)
5. [ ] `UlidArgumentResolver` - @PathVariable/@RequestParam
6. [ ] `StringToUuidConverterFactory` - @ModelAttribute/Form
7. [ ] `UlidWebMvcConfig` - 설정 등록
8. [ ] `UlidOpenApiConfig` - Swagger 문서화

### Phase 3: 검증
9. [ ] 통합 테스트
10. [ ] 기존 API 호환성 테스트

---

## 7. 영향 범위

### 변경되는 것
- API 응답의 UUID가 ULID 형식으로 변경
- Swagger 문서의 ID 예시가 ULID로 변경

### 변경되지 않는 것
- Entity 클래스
- Repository 인터페이스
- Service 클래스
- 데이터베이스 스키마

### 하위 호환성
- 기존 UUID 형식 입력 계속 지원
- 클라이언트가 UUID로 요청해도 정상 동작

---

## 8. 참고

- 기존 라이브러리: `com.github.f4b6a3:ulid-creator:5.2.0` (이미 설치됨)
- ULID Spec: https://github.com/ulid/spec
- Crockford Base32: I, L, O, U 제외 (혼동 방지)
