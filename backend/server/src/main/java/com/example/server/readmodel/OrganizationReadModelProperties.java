package com.example.server.readmodel;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis 기반 조직 Read Model 저장소 설정값.
 */
@ConfigurationProperties(prefix = "readmodel.organization")
public class OrganizationReadModelProperties {

    private boolean enabled = false;
    private String keyPrefix = "rm:org";
    private String tenantId = "default";
    private Duration ttl = Duration.ofMinutes(30);
    private boolean refreshOnMiss = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public boolean isRefreshOnMiss() {
        return refreshOnMiss;
    }

    public void setRefreshOnMiss(boolean refreshOnMiss) {
        this.refreshOnMiss = refreshOnMiss;
    }
}
