package com.example.admin.permission.declarative;

import com.example.admin.permission.domain.PermissionAssignment;
import com.example.admin.permission.domain.PermissionGroup;
import com.example.admin.permission.domain.PermissionGroupRoot;
import com.example.admin.permission.repository.PermissionGroupRepository;
import com.example.admin.permission.repository.PermissionGroupRootRepository;
import com.example.common.version.ChangeAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * YAML 기반 선언형 PermissionGroup 로더.
 * <p>
 * 애플리케이션 시작 시 권한 정의 YAML 파일을 읽어 PermissionGroup을 동기화합니다.
 * 버전 관리 적용됨 - PermissionGroupRoot와 PermissionGroup을 함께 관리합니다.
 * </p>
 */
@Component
@Slf4j
class DeclarativePermissionGroupLoader implements ApplicationRunner {

  private final PermissionGroupRootRepository rootRepository;
  private final PermissionGroupRepository versionRepository;
  private final DeclarativePermissionProperties properties;
  private final ResourceLoader resourceLoader;
  private final ObjectMapper yamlMapper;

  DeclarativePermissionGroupLoader(
      PermissionGroupRootRepository rootRepository,
      PermissionGroupRepository versionRepository,
      DeclarativePermissionProperties properties,
      ResourceLoader resourceLoader,
      @Lazy @Qualifier("permissionPolicyYamlMapper") ObjectMapper permissionPolicyYamlMapper) {
    this.rootRepository = rootRepository;
    this.versionRepository = versionRepository;
    this.properties = properties;
    this.resourceLoader = resourceLoader;
    this.yamlMapper = permissionPolicyYamlMapper;
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

  @Transactional
  void synchronize(List<PermissionGroupDefinition> definitions) {
    OffsetDateTime now = OffsetDateTime.now();
    
    for (PermissionGroupDefinition definition : definitions) {
      Assert.hasText(definition.code(), "PermissionGroup code는 필수입니다.");
      Assert.hasText(definition.name(), "PermissionGroup name은 필수입니다.");

      PermissionGroupRoot root = rootRepository.findByGroupCode(definition.code())
          .orElseGet(() -> {
            PermissionGroupRoot newRoot = PermissionGroupRoot.createWithCode(definition.code(), now);
            return rootRepository.save(newRoot);
          });

      List<PermissionAssignment> assignments = toAssignments(definition.assignmentsOrEmpty());
      List<String> approvalGroupCodes = definition.approvalGroupCodesOrEmpty();

      int nextVersion = versionRepository.findMaxVersionByRootId(root.getId()) + 1;
      
      PermissionGroup group = PermissionGroup.create(
          root,
          nextVersion,
          definition.name(),
          definition.description(),
          true, // active
          assignments,
          approvalGroupCodes,
          ChangeAction.CREATE,
          "선언형 정의 동기화",
          "SYSTEM",
          "System",
          now);
      
      versionRepository.save(group);
      root.activateNewVersion(group, now);
      rootRepository.save(root);
      
      log.debug("PermissionGroup [{}] 동기화 완료 (version={})", definition.code(), nextVersion);
    }
  }

  private List<PermissionAssignment> toAssignments(
      List<PermissionAssignmentDefinition> definitions) {
    List<PermissionAssignment> assignments = new ArrayList<>();
    for (PermissionAssignmentDefinition definition : definitions) {
      Assert.notNull(definition.feature(), "feature는 필수입니다.");
      Assert.notNull(definition.action(), "action은 필수입니다.");
      assignments.add(new PermissionAssignment(definition.feature(), definition.action()));
    }
    return assignments;
  }
}
