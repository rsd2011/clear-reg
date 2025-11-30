package com.example.admin.permission.domain;

import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.List;

/**
 * PermissionAssignment 목록을 JSON으로 변환하는 JPA Converter.
 * <p>
 * PostgreSQL의 JSONB 컬럼에 저장됩니다.
 * </p>
 */
@Converter
public class PermissionAssignmentListConverter
    implements AttributeConverter<List<PermissionAssignment>, String> {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(List<PermissionAssignment> attribute) {
    if (attribute == null || attribute.isEmpty()) {
      return "[]";
    }
    try {
      List<AssignmentDto> dtos = attribute.stream()
          .map(a -> new AssignmentDto(a.getFeature().name(), a.getAction().name()))
          .toList();
      return objectMapper.writeValueAsString(dtos);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Failed to convert assignments to JSON", e);
    }
  }

  @Override
  public List<PermissionAssignment> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank() || "[]".equals(dbData)) {
      return new ArrayList<>();
    }
    try {
      List<AssignmentDto> dtos = objectMapper.readValue(
          dbData, new TypeReference<List<AssignmentDto>>() {});
      return dtos.stream()
          .map(dto -> new PermissionAssignment(
              FeatureCode.valueOf(dto.feature()),
              ActionCode.valueOf(dto.action())))
          .toList();
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Failed to convert JSON to assignments", e);
    }
  }

  /**
   * JSON 직렬화를 위한 DTO.
   */
  private record AssignmentDto(String feature, String action) {}
}
