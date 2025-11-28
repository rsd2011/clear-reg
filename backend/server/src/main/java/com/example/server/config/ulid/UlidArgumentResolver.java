package com.example.server.config.ulid;

import java.util.UUID;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.example.common.ulid.UlidUtils;

/**
 * UUID 타입 @PathVariable, @RequestParam에 대해 ULID/UUID 문자열을 UUID로 변환하는 ArgumentResolver.
 *
 * <p>Spring MVC의 기본 UUID 변환은 UUID 형식만 지원하므로,
 * ULID 형식도 지원하기 위해 커스텀 resolver가 필요합니다.</p>
 *
 * <h3>지원하는 변환</h3>
 * <ul>
 *   <li>ULID (26자): {@code 01ARZ3NDEKTSV4RRFFQ69G5FAV} → UUID</li>
 *   <li>UUID (36자): {@code 550e8400-e29b-41d4-a716-446655440000} → UUID (하위 호환)</li>
 * </ul>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * @GetMapping("/users/{id}")
 * public User getUser(@PathVariable UUID id) {
 *     // id는 ULID 또는 UUID 문자열에서 변환됨
 * }
 *
 * @GetMapping("/users")
 * public List<User> searchUsers(@RequestParam UUID departmentId) {
 *     // departmentId는 ULID 또는 UUID 문자열에서 변환됨
 * }
 * }</pre>
 *
 * @see UlidUtils
 * @see UlidWebMvcConfig
 */
public class UlidArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return UUID.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) throws Exception {

        String parameterName = parameter.getParameterName();
        if (parameterName == null) {
            return null;
        }

        String value = webRequest.getParameter(parameterName);
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return UlidUtils.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Failed to convert parameter '" + parameterName + "' with value '" + value + "': " + e.getMessage(),
                e
            );
        }
    }
}
