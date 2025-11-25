package com.example.auth.permission.declarative;

import com.example.auth.permission.FieldMaskRule;
import com.example.auth.permission.PermissionAssignment;
import com.example.auth.permission.PermissionGroup;
import com.example.auth.permission.PermissionGroupRepository;
import com.example.auth.permission.RowConditionEvaluator;
import com.example.common.security.RowScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@Slf4j
class DeclarativePermissionGroupLoader implements ApplicationRunner {

  private final PermissionGroupRepository repository;
  private final DeclarativePermissionProperties properties;
  private final ResourceLoader resourceLoader;
  private final ObjectMapper yamlMapper;
  private final RowConditionEvaluator rowConditionEvaluator;

  DeclarativePermissionGroupLoader(
      PermissionGroupRepository repository,
      DeclarativePermissionProperties properties,
      ResourceLoader resourceLoader,
      @Lazy @Qualifier("permissionPolicyYamlMapper") ObjectMapper permissionPolicyYamlMapper,
      RowConditionEvaluator rowConditionEvaluator) {
    this.repository = repository;
    this.properties = properties;
    this.resourceLoader = resourceLoader;
    this.yamlMapper = permissionPolicyYamlMapper;
    this.rowConditionEvaluator = rowConditionEvaluator;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!properties.isEnabled()) {
      log.info("선언형 PermissionGroup 동기화가 비활성화되어 실행하지 않습니다.");
      return;
    }
    Resource resource = resourceLoader.getResource(properties.getLocation());
    if (!resource.exists()) {
      String message = "권한 정의 파일을 찾을 수 없습니다: " + properties.getLocation();
      if (properties.isFailOnMissingFile()) {
        throw new IllegalStateException(message);
      }
      log.warn(message);
      return;
    }
    try (InputStream inputStream = resource.getInputStream()) {
      PermissionCatalogDefinition catalog =
          yamlMapper.readValue(inputStream, PermissionCatalogDefinition.class);
      synchronize(catalog.groupsOrEmpty());
    } catch (IOException ex) {
      throw new IllegalStateException("권한 정의 파일을 읽는 중 오류가 발생했습니다.", ex);
    }
  }

  void synchronize(List<PermissionGroupDefinition> definitions) {
    for (PermissionGroupDefinition definition : definitions) {
      Assert.hasText(definition.code(), "PermissionGroup code는 필수입니다.");
      Assert.hasText(definition.name(), "PermissionGroup name은 필수입니다.");

      PermissionGroup group =
          repository
              .findByCode(definition.code())
              .orElseGet(() -> new PermissionGroup(definition.code(), definition.name()));

      group.updateDetails(
          definition.name(),
          definition.description(),
          definition.defaultRowScope() == null ? RowScope.OWN : definition.defaultRowScope());
      group.replaceAssignments(toAssignments(definition.assignmentsOrEmpty()));
      group.replaceMaskRules(toMaskRules(definition.maskRulesOrEmpty()));
      repository.save(group);
      log.debug("PermissionGroup [{}] 동기화 완료", group.getCode());
    }
  }

  private Set<PermissionAssignment> toAssignments(
      List<PermissionAssignmentDefinition> definitions) {
    Set<PermissionAssignment> assignments = new LinkedHashSet<>();
    for (PermissionAssignmentDefinition definition : definitions) {
      Assert.notNull(definition.feature(), "feature는 필수입니다.");
      Assert.notNull(definition.action(), "action은 필수입니다.");
      RowScope rowScope = definition.rowScope() == null ? RowScope.OWN : definition.rowScope();
      String condition = definition.condition();
      rowConditionEvaluator.validate(condition);
      assignments.add(
          new PermissionAssignment(definition.feature(), definition.action(), rowScope, condition));
    }
    return assignments;
  }

  private Set<FieldMaskRule> toMaskRules(List<FieldMaskRuleDefinition> definitions) {
    Set<FieldMaskRule> rules = new LinkedHashSet<>();
    for (FieldMaskRuleDefinition definition : definitions) {
      Assert.hasText(definition.tag(), "mask rule tag는 필수입니다.");
      rules.add(definition.toMaskRule());
    }
    return rules;
  }
}
