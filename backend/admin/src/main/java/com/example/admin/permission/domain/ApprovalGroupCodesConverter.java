package com.example.admin.permission.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.List;

/**
 * 승인 그룹 코드 목록을 JSON으로 변환하는 JPA Converter.
 * <p>
 * PostgreSQL의 JSONB 컬럼에 저장됩니다.
 * </p>
 */
@Converter
public class ApprovalGroupCodesConverter
    implements AttributeConverter<List<String>, String> {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(List<String> attribute) {
    if (attribute == null || attribute.isEmpty()) {
      return "[]";
    }
    try {
      return objectMapper.writeValueAsString(attribute);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Failed to convert approval group codes to JSON", e);
    }
  }

  @Override
  public List<String> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank() || "[]".equals(dbData)) {
      return new ArrayList<>();
    }
    try {
      return objectMapper.readValue(dbData, new TypeReference<List<String>>() {});
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Failed to convert JSON to approval group codes", e);
    }
  }
}
