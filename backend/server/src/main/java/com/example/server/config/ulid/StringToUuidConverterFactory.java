package com.example.server.config.ulid;

import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.lang.NonNull;

import com.example.common.ulid.UlidUtils;

/**
 * String을 UUID로 변환하는 Spring ConverterFactory.
 *
 * <p>ULID 또는 UUID 형식의 문자열을 UUID로 변환합니다.
 * 이 컨버터는 @PathVariable, @RequestParam, form 데이터 바인딩 등에서 사용됩니다.</p>
 *
 * <h3>지원하는 변환</h3>
 * <ul>
 *   <li>ULID (26자): {@code 01ARZ3NDEKTSV4RRFFQ69G5FAV}</li>
 *   <li>UUID (36자): {@code 550e8400-e29b-41d4-a716-446655440000}</li>
 * </ul>
 *
 * @see UlidUtils
 * @see UlidWebMvcConfig
 */
public class StringToUuidConverterFactory implements ConverterFactory<String, UUID> {

    @Override
    @NonNull
    @SuppressWarnings({"unchecked", "TypeParameterExtendsFinalClass"})
    public <T extends UUID> Converter<String, T> getConverter(@NonNull Class<T> targetType) {
        return (Converter<String, T>) new StringToUuidConverter();
    }

    private static class StringToUuidConverter implements Converter<String, UUID> {

        @Override
        public UUID convert(@NonNull String source) {
            if (source.isBlank()) {
                return null;
            }
            return UlidUtils.fromString(source);
        }
    }
}
