package com.example.admin.systemconfig.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SystemConfigChangedEvent 테스트")
class SystemConfigChangedEventTest {

  @Nested
  @DisplayName("생성자 테스트")
  class ConstructorTest {

    @Test
    @DisplayName("Given: configCode와 yaml, When: 생성자 호출, Then: 필드가 올바르게 설정됨")
    void shouldCreateEventWithFields() {
      // Given
      String configCode = "auth.settings";
      String yaml = "passwordPolicyEnabled: true";

      // When
      SystemConfigChangedEvent event = new SystemConfigChangedEvent(configCode, yaml);

      // Then
      assertThat(event.getConfigCode()).isEqualTo("auth.settings");
      assertThat(event.getYaml()).isEqualTo("passwordPolicyEnabled: true");
    }

    @Test
    @DisplayName("Given: null yaml, When: 생성자 호출, Then: 이벤트 생성됨")
    void shouldCreateEventWithNullYaml() {
      // Given
      String configCode = "file.settings";

      // When
      SystemConfigChangedEvent event = new SystemConfigChangedEvent(configCode, null);

      // Then
      assertThat(event.getConfigCode()).isEqualTo("file.settings");
      assertThat(event.getYaml()).isNull();
    }
  }

  @Nested
  @DisplayName("getSource 테스트")
  class GetSourceTest {

    @Test
    @DisplayName("Given: 생성된 이벤트, When: getSource 호출, Then: configCode 반환")
    void shouldReturnConfigCodeAsSource() {
      // Given
      SystemConfigChangedEvent event = new SystemConfigChangedEvent("audit.settings", "yaml");

      // When
      Object source = event.getSource();

      // Then
      assertThat(source).isEqualTo("audit.settings");
    }
  }

  @Nested
  @DisplayName("toString 테스트")
  class ToStringTest {

    @Test
    @DisplayName("Given: 생성된 이벤트, When: toString 호출, Then: 포맷된 문자열 반환")
    void shouldReturnFormattedString() {
      // Given
      SystemConfigChangedEvent event = new SystemConfigChangedEvent("auth.settings", "yaml");

      // When
      String result = event.toString();

      // Then
      assertThat(result).contains("SystemConfigChangedEvent");
      assertThat(result).contains("auth.settings");
    }
  }

  @Nested
  @DisplayName("설정 코드별 이벤트 테스트")
  class ConfigCodeSpecificTest {

    @Test
    @DisplayName("auth.settings 이벤트 생성")
    void shouldCreateAuthSettingsEvent() {
      // Given
      String yaml = """
          passwordPolicyEnabled: true
          accountLockEnabled: true
          """;

      // When
      SystemConfigChangedEvent event = new SystemConfigChangedEvent(
          "auth.settings", yaml);

      // Then
      assertThat(event.getConfigCode()).isEqualTo("auth.settings");
      assertThat(event.getYaml()).contains("passwordPolicyEnabled");
    }

    @Test
    @DisplayName("file.settings 이벤트 생성")
    void shouldCreateFileSettingsEvent() {
      // Given
      String yaml = """
          maxFileSizeBytes: 10485760
          strictMimeValidation: true
          """;

      // When
      SystemConfigChangedEvent event = new SystemConfigChangedEvent(
          "file.settings", yaml);

      // Then
      assertThat(event.getConfigCode()).isEqualTo("file.settings");
      assertThat(event.getYaml()).contains("maxFileSizeBytes");
    }

    @Test
    @DisplayName("audit.settings 이벤트 생성")
    void shouldCreateAuditSettingsEvent() {
      // Given
      String yaml = """
          auditEnabled: true
          auditRetentionDays: 90
          """;

      // When
      SystemConfigChangedEvent event = new SystemConfigChangedEvent(
          "audit.settings", yaml);

      // Then
      assertThat(event.getConfigCode()).isEqualTo("audit.settings");
      assertThat(event.getYaml()).contains("auditEnabled");
    }
  }
}
