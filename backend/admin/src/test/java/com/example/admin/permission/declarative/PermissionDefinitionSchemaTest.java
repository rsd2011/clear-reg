package com.example.admin.permission.declarative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.example.admin.permission.RowConditionEvaluator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("permission-groups.yml 스키마 검증")
class PermissionDefinitionSchemaTest {

  private static final Path YAML_PATH = Path.of("src/main/resources/permission-groups.yml");

  private final ObjectMapper mapper =
      new ObjectMapper(new YAMLFactory()).registerModule(new JavaTimeModule());
  private final RowConditionEvaluator rowConditionEvaluator = new RowConditionEvaluator();

  @Test
  @DisplayName("권한 정의가 중복 없이 필수 항목을 채운다")
  void validatePermissionDefinitions() throws IOException {
    PermissionCatalogDefinition catalog;
    try (var inputStream = Files.newInputStream(YAML_PATH)) {
      catalog = mapper.readValue(inputStream, PermissionCatalogDefinition.class);
    }

    assertThat(catalog).as("permission-groups.yml parsing result").isNotNull();
    List<PermissionGroupDefinition> groups = catalog.groupsOrEmpty();
    assertThat(groups).isNotEmpty();

    Set<String> groupCodes = new HashSet<>();
    for (PermissionGroupDefinition definition : groups) {
      assertThat(definition.code()).describedAs("group code").isNotBlank();
      assertThat(definition.name()).describedAs("name for %s", definition.code()).isNotBlank();
      assertThat(groupCodes.add(definition.code()))
          .describedAs("duplicate group code: %s", definition.code())
          .isTrue();

      validateAssignments(definition);
      validateMaskRules(definition);
    }
  }

  private void validateAssignments(PermissionGroupDefinition definition) {
    for (PermissionAssignmentDefinition assignment : definition.assignmentsOrEmpty()) {
      assertThat(assignment.feature()).describedAs("feature for %s", definition.code()).isNotNull();
      assertThat(assignment.action()).describedAs("action for %s", definition.code()).isNotNull();
      assertThatNoException()
          .describedAs("row condition for %s", definition.code())
          .isThrownBy(() -> rowConditionEvaluator.validate(assignment.condition()));
    }
  }

  private void validateMaskRules(PermissionGroupDefinition definition) {
    for (FieldMaskRuleDefinition rule : definition.maskRulesOrEmpty()) {
      assertThat(rule.tag()).describedAs("mask rule tag for %s", definition.code()).isNotBlank();
    }
  }
}
