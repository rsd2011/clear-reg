package com.example.batch.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "cache.invalidation")
@Getter
@Setter
public class CacheInvalidationProperties {

    private boolean enabled = false;
    private String channel = "cache-invalidation";

}
