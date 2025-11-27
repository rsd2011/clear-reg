package com.example.admin.permission.declarative;

import static org.assertj.core.api.Assertions.assertThat;

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

  private DeclarativePermissionGroupLoader loader;

  @BeforeEach
  void setUp() {
    DeclarativePermissionProperties properties = new DeclarativePermissionProperties();
    properties.setLocation("classpath:permissions/test-permission-groups.yml");
    loader =
        new DeclarativePermissionGroupLoader(
            repository,
            properties,
            new DefaultResourceLoader(),
            yamlMapper(),
            new RowConditionEvaluator());
  }

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
