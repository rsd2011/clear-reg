package com.example.admin.systemconfig.event;

import org.springframework.context.ApplicationEvent;

/**
 * 시스템 설정이 변경되었을 때 발행되는 이벤트.
 * <p>
 * 다른 모듈에서 이 이벤트를 구독하여 설정 변경에 반응할 수 있습니다.
 * </p>
 */
public class SystemConfigChangedEvent extends ApplicationEvent {

  private final String configCode;
  private final String yaml;

  /**
   * 새 SystemConfigChangedEvent를 생성합니다.
   *
   * @param configCode 변경된 설정 코드 (예: auth.settings, file.settings, audit.settings)
   * @param yaml 새 YAML 내용
   */
  public SystemConfigChangedEvent(String configCode, String yaml) {
    super(configCode);
    this.configCode = configCode;
    this.yaml = yaml;
  }

  /**
   * 변경된 설정 코드를 반환합니다.
   */
  public String getConfigCode() {
    return configCode;
  }

  /**
   * 새 YAML 내용을 반환합니다.
   */
  public String getYaml() {
    return yaml;
  }

  @Override
  public String toString() {
    return "SystemConfigChangedEvent{configCode='" + configCode + "'}";
  }
}
