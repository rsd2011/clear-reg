package com.example.admin.permission.declarative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import com.example.admin.permission.PermissionGroup;
import com.example.admin.permission.PermissionGroupRepository;
import com.example.admin.permission.RowConditionEvaluator;
import com.example.common.security.RowScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
@DisplayName("DeclarativePermissionGroupLoader 테스트")
class DeclarativePermissionGroupLoaderTest {

  @Autowired private PermissionGroupRepository repository;

  private DeclarativePermissionProperties properties;
  private DeclarativePermissionGroupLoader loader;

  @BeforeEach
  void setUp() {
    properties = new DeclarativePermissionProperties();
    properties.setLocation("classpath:permissions/test-permission-groups.yml");
    loader =
        new DeclarativePermissionGroupLoader(
            repository,
            properties,
            new DefaultResourceLoader(),
            yamlMapper(),
            new RowConditionEvaluator());
  }

  @Nested
  @DisplayName("run 메서드")
  class Run {

    @Test
    @DisplayName("Given YAML 정의 When 로더 실행 Then 권한 그룹이 저장된다")
    void givenYamlDefinitions_whenLoaderRuns_thenPermissionGroupsPersisted() throws Exception {
      loader.run(new DefaultApplicationArguments(new String[0]));

      PermissionGroup defaultGroup = repository.findByCode("TEST_DEFAULT").orElseThrow();
      assertThat(defaultGroup.getName()).isEqualTo("테스트 기본");
      assertThat(defaultGroup.getDefaultRowScope()).isEqualTo(RowScope.ORG);
      assertThat(defaultGroup.getAssignments())
          .anySatisfy(
              assignment -> assertThat(assignment.getFeature()).isEqualTo(FeatureCode.ORGANIZATION));
      assertThat(defaultGroup.getAssignments())
          .anySatisfy(
              assignment ->
                  assertThat(assignment.getRowConditionExpression())
                      .contains("organizationCode == 'ROOT'"));
      assertThat(repository.findByCode("TEST_AUDIT")).isPresent();
    }

    @Test
    @DisplayName("Given enabled=false When run 실행 Then 아무 동작 없이 반환")
    void givenDisabled_whenRun_thenNoAction() throws Exception {
      properties.setEnabled(false);

      loader.run(new DefaultApplicationArguments(new String[0]));

      assertThat(repository.findByCode("TEST_DEFAULT")).isEmpty();
    }

    @Test
    @DisplayName("Given 존재하지 않는 파일 + failOnMissingFile=true When run 실행 Then 예외 발생")
    void givenMissingFileWithFailFlag_whenRun_thenThrowsException() {
      properties.setLocation("classpath:permissions/non-existent.yml");
      properties.setFailOnMissingFile(true);

      assertThatThrownBy(() -> loader.run(new DefaultApplicationArguments(new String[0])))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("권한 정의 파일을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("Given 존재하지 않는 파일 + failOnMissingFile=false When run 실행 Then 경고만 출력하고 반환")
    void givenMissingFileWithoutFailFlag_whenRun_thenWarnsAndReturns() throws Exception {
      properties.setLocation("classpath:permissions/non-existent.yml");
      properties.setFailOnMissingFile(false);

      // 예외 없이 실행되어야 함
      loader.run(new DefaultApplicationArguments(new String[0]));

      assertThat(repository.findByCode("TEST_DEFAULT")).isEmpty();
    }
  }

  @Nested
  @DisplayName("synchronize 메서드")
  class Synchronize {

    @Test
    @DisplayName("Given null defaultRowScope When synchronize Then OWN이 기본값으로 설정된다")
    void givenNullDefaultRowScope_whenSynchronize_thenUsesOwnAsDefault() {
      PermissionGroupDefinition definition = new PermissionGroupDefinition(
          "NEW_GROUP", "새 그룹", "설명", null, List.of()
      );

      loader.synchronize(List.of(definition));

      PermissionGroup group = repository.findByCode("NEW_GROUP").orElseThrow();
      assertThat(group.getDefaultRowScope()).isEqualTo(RowScope.OWN);
    }

    @Test
    @DisplayName("Given 명시적 defaultRowScope When synchronize Then 해당 값이 설정된다")
    void givenExplicitDefaultRowScope_whenSynchronize_thenUsesGivenValue() {
      PermissionGroupDefinition definition = new PermissionGroupDefinition(
          "NEW_GROUP", "새 그룹", "설명", RowScope.ALL, List.of()
      );

      loader.synchronize(List.of(definition));

      PermissionGroup group = repository.findByCode("NEW_GROUP").orElseThrow();
      assertThat(group.getDefaultRowScope()).isEqualTo(RowScope.ALL);
    }

    @Test
    @DisplayName("Given 기존 그룹 존재 When synchronize Then 그룹이 업데이트된다")
    void givenExistingGroup_whenSynchronize_thenUpdates() {
      // 먼저 그룹 생성
      PermissionGroup existing = new PermissionGroup("EXISTING", "기존 그룹");
      repository.save(existing);

      // 업데이트
      PermissionGroupDefinition definition = new PermissionGroupDefinition(
          "EXISTING", "업데이트된 그룹", "새 설명", RowScope.ORG, List.of()
      );
      loader.synchronize(List.of(definition));

      PermissionGroup updated = repository.findByCode("EXISTING").orElseThrow();
      assertThat(updated.getName()).isEqualTo("업데이트된 그룹");
      assertThat(updated.getDescription()).isEqualTo("새 설명");
      assertThat(updated.getDefaultRowScope()).isEqualTo(RowScope.ORG);
    }

    @Test
    @DisplayName("Given null rowScope in assignment When synchronize Then OWN이 기본값으로 설정된다")
    void givenNullRowScopeInAssignment_whenSynchronize_thenUsesOwnAsDefault() {
      PermissionAssignmentDefinition assignment = new PermissionAssignmentDefinition(
          FeatureCode.DRAFT, ActionCode.READ, null, null
      );
      PermissionGroupDefinition definition = new PermissionGroupDefinition(
          "GROUP_WITH_ASSIGN", "그룹", null, null, List.of(assignment)
      );

      loader.synchronize(List.of(definition));

      PermissionGroup group = repository.findByCode("GROUP_WITH_ASSIGN").orElseThrow();
      assertThat(group.getAssignments())
          .anySatisfy(a -> assertThat(a.getRowScope()).isEqualTo(RowScope.OWN));
    }

    @Test
    @DisplayName("Given 명시적 rowScope in assignment When synchronize Then 해당 값이 설정된다")
    void givenExplicitRowScopeInAssignment_whenSynchronize_thenUsesGivenValue() {
      PermissionAssignmentDefinition assignment = new PermissionAssignmentDefinition(
          FeatureCode.DRAFT, ActionCode.CREATE, RowScope.ALL, null
      );
      PermissionGroupDefinition definition = new PermissionGroupDefinition(
          "GROUP_WITH_ALL", "그룹", null, null, List.of(assignment)
      );

      loader.synchronize(List.of(definition));

      PermissionGroup group = repository.findByCode("GROUP_WITH_ALL").orElseThrow();
      assertThat(group.getAssignments())
          .anySatisfy(a -> assertThat(a.getRowScope()).isEqualTo(RowScope.ALL));
    }
  }

  private ObjectMapper yamlMapper() {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    mapper.registerModule(new JavaTimeModule());
    return mapper;
  }

  @SpringBootApplication(scanBasePackages = "com.example")
  @EntityScan(basePackages = "com.example")
  @EnableJpaRepositories(basePackages = "com.example")
  static class TestApplication {}
}
