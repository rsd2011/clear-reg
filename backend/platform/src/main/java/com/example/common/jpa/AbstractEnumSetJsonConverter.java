package com.example.common.jpa;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;

import jakarta.persistence.AttributeConverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Enum Set을 JSON 배열로 저장하는 제네릭 JPA Converter.
 *
 * <p>저장 형식: ["VALUE1", "VALUE2", "VALUE3"]
 *
 * <p>동작:
 * <ul>
 *   <li>빈 Set → "[]"</li>
 *   <li>null → "[]"</li>
 *   <li>Set.of(VALUE1, VALUE2) → '["VALUE1","VALUE2"]'</li>
 * </ul>
 *
 * <p>사용법:
 * <pre>{@code
 * @Converter
 * public class WorkCategorySetConverter
 *         extends AbstractEnumSetJsonConverter<WorkCategory> {
 *     public WorkCategorySetConverter() {
 *         super(WorkCategory.class, WorkCategory::fromString);
 *     }
 * }
 * }</pre>
 *
 * @param <E> Enum 타입
 */
public abstract class AbstractEnumSetJsonConverter<E extends Enum<E>>
        implements AttributeConverter<Set<E>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Set<String>> SET_TYPE = new TypeReference<>() {};

    private final Class<E> enumClass;
    private final Function<String, E> parser;

    /**
     * 컨버터를 생성한다.
     *
     * @param enumClass Enum 클래스
     * @param parser 문자열을 Enum으로 변환하는 함수 (null 반환 가능)
     */
    protected AbstractEnumSetJsonConverter(Class<E> enumClass, Function<String, E> parser) {
        this.enumClass = enumClass;
        this.parser = parser;
    }

    @Override
    public String convertToDatabaseColumn(Set<E> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(
                    attribute.stream()
                            .map(Enum::name)
                            .toList()
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize " + enumClass.getSimpleName() + " set to JSON", e);
        }
    }

    @Override
    public Set<E> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank() || "[]".equals(dbData.trim())) {
            return Collections.emptySet();
        }
        try {
            Set<String> names = MAPPER.readValue(dbData, SET_TYPE);
            if (names.isEmpty()) {
                return Collections.emptySet();
            }
            EnumSet<E> result = EnumSet.noneOf(enumClass);
            for (String name : names) {
                E value = parser.apply(name);
                if (value != null) {
                    result.add(value);
                }
            }
            return result.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(result);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to deserialize " + enumClass.getSimpleName() + " set from JSON: " + dbData, e);
        }
    }

    /**
     * Enum 클래스를 반환한다.
     *
     * @return Enum 클래스
     */
    protected Class<E> getEnumClass() {
        return enumClass;
    }
}
