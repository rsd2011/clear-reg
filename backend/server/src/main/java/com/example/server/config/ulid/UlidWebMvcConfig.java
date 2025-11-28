package com.example.server.config.ulid;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.common.ulid.UlidJacksonModule;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

/**
 * ULID 지원을 위한 Spring MVC 설정.
 *
 * <p>이 설정을 통해 다음 기능이 활성화됩니다:</p>
 * <ul>
 *   <li>@PathVariable, @RequestParam의 UUID 파라미터가 ULID/UUID 문자열을 모두 지원</li>
 *   <li>@RequestBody의 UUID 필드가 ULID/UUID 문자열을 모두 지원</li>
 *   <li>응답 JSON의 UUID 필드가 ULID 형식으로 직렬화</li>
 * </ul>
 *
 * <h3>API 사용 예시</h3>
 * <pre>{@code
 * // Request - ULID 형식
 * GET /api/v1/users/01ARZ3NDEKTSV4RRFFQ69G5FAV
 *
 * // Request - UUID 형식 (하위 호환)
 * GET /api/v1/users/550e8400-e29b-41d4-a716-446655440000
 *
 * // Response - 항상 ULID 형식
 * {
 *   "id": "01ARZ3NDEKTSV4RRFFQ69G5FAV",
 *   "name": "홍길동"
 * }
 * }</pre>
 *
 * @see UlidArgumentResolver
 * @see StringToUuidConverterFactory
 * @see UlidJacksonModule
 */
@Configuration
public class UlidWebMvcConfig implements WebMvcConfigurer {

    private final ObjectMapper objectMapper;

    public UlidWebMvcConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void configureJackson() {
        // Jackson ObjectMapper에 ULID 모듈 등록
        objectMapper.registerModule(new UlidJacksonModule());
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new UlidArgumentResolver());
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new StringToUuidConverterFactory());
    }
}
