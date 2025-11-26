package com.example.server.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "cache.invalidation")
@Getter
@Setter
public class CacheInvalidationProperties {

    /**
     * 캐시 무효화 채널 활성화 여부.
     */
    private boolean enabled = true;

    /**
     * Redis Pub/Sub 채널명.
     */
    private String channel = "cache-invalidation";

}
