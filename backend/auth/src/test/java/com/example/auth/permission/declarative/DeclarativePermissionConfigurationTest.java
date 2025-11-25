package com.example.auth.permission.declarative;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DeclarativePermissionConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(DeclarativePermissionConfiguration.class));

  @Test
  @DisplayName("권한 선언 설정이 로드되면 YAML 전용 ObjectMapper 빈을 생성한다")
  void createsYamlMapperBean() {
    contextRunner.run(
        context -> {
          assertThat(context).hasSingleBean(ObjectMapper.class);
          assertThat(context.getBean("permissionPolicyYamlMapper"))
              .isInstanceOf(ObjectMapper.class);
        });
  }
}
