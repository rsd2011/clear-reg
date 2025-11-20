package com.example.server.readmodel;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "readmodel.permission-menu")
public class PermissionMenuReadModelProperties {

    private boolean enabled = false;
    private String keyPrefix = "rm:perm-menu";
    private String tenantId = "default";
    private boolean refreshOnMiss = true;
    private Duration ttl = Duration.ofMinutes(10);

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

    public boolean isRefreshOnMiss() {
        return refreshOnMiss;
    }

    public void setRefreshOnMiss(boolean refreshOnMiss) {
        this.refreshOnMiss = refreshOnMiss;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }
}
