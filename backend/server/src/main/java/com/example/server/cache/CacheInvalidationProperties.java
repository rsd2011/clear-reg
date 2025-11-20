package com.example.server.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cache.invalidation")
public class CacheInvalidationProperties {

    /**
     * 캐시 무효화 채널 활성화 여부.
     */
    private boolean enabled = true;

    /**
     * Redis Pub/Sub 채널명.
     */
    private String channel = "cache-invalidation";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }
}
