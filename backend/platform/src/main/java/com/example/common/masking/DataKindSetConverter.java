package com.example.common.masking;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Set&lt;DataKind&gt;를 JSON 문자열로 DB에 저장하는 JPA Converter.
 *
 * <p>저장 형식: ["SSN", "PHONE", "EMAIL"]
 *
 * <p>동작:
 * <ul>
 *   <li>빈 Set → "[]" (모든 DataKind에 적용됨을 의미)</li>
 *   <li>null → "[]"</li>
 *   <li>Set.of(SSN, PHONE) → '["SSN","PHONE"]'</li>
 * </ul>
 */
@Converter
public class DataKindSetConverter implements AttributeConverter<Set<DataKind>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Set<String>> SET_TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(Set<DataKind> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(
                    attribute.stream()
                            .map(DataKind::name)
                            .toList()
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize DataKind set to JSON", e);
        }
    }

    @Override
    public Set<DataKind> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank() || "[]".equals(dbData.trim())) {
            return Collections.emptySet();
        }
        try {
            Set<String> names = MAPPER.readValue(dbData, SET_TYPE);
            if (names.isEmpty()) {
                return Collections.emptySet();
            }
            EnumSet<DataKind> result = EnumSet.noneOf(DataKind.class);
            for (String name : names) {
                result.add(DataKind.fromString(name));
            }
            return Collections.unmodifiableSet(result);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to deserialize DataKind set from JSON: " + dbData, e);
        }
    }
}
