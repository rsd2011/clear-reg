package com.example.auth.permission.declarative;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.permission.declarative")
public class DeclarativePermissionProperties {

    /**
     * 플래그가 false면 선언형 동기화 동작을 비활성화한다.
     */
    private boolean enabled = true;

    /**
     * 권한 정의 YAML이 위치한 Resource 표현식 (예: classpath:, file:).
     */
    private String location = "classpath:permission-groups.yml";

    /**
     * true면 파일이 없을 때 애플리케이션 구동을 중단한다.
     */
    private boolean failOnMissingFile;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isFailOnMissingFile() {
        return failOnMissingFile;
    }

    public void setFailOnMissingFile(boolean failOnMissingFile) {
        this.failOnMissingFile = failOnMissingFile;
    }
}
