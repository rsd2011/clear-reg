package com.example.server.config;

import java.time.Duration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import java.util.List;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.example.common.cache.CacheNames;

@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheTtlProperties.class)
public class CacheConfig {

    @Bean
    public Caffeine<Object, Object> caffeine(CacheTtlProperties properties) {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(properties.getLocalTtlSeconds()))
                .maximumSize(properties.getLocalMaximumSize());
    }

    @Bean
    public CaffeineCacheManager caffeineCacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager manager = new CaffeineCacheManager(CacheNames.USER_DETAILS,
                CacheNames.GREETINGS,
                CacheNames.ORGANIZATION_POLICIES,
                CacheNames.USER_ACCOUNTS,
                CacheNames.LATEST_DW_BATCH,
                CacheNames.ORGANIZATION_ROW_SCOPE,
                CacheNames.DW_EMPLOYEES,
                CacheNames.DW_ORG_TREE,
                CacheNames.DW_COMMON_CODES,
                CacheNames.SYSTEM_COMMON_CODES,
                CacheNames.COMMON_CODE_AGGREGATES);
        manager.setCaffeine(caffeine);
        return manager;
    }

    @Bean
    @ConditionalOnProperty(prefix = "cache.redis", name = "enabled", havingValue = "true")
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory,
                                               CacheTtlProperties properties) {
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(properties.getRedisTtlSeconds()));
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(configuration)
                .build();
    }

    @Bean
    @Primary
    public CacheManager cacheManager(CaffeineCacheManager caffeineCacheManager,
                                     ObjectProvider<RedisCacheManager> redisCacheManager) {
        CacheManager[] managers = redisCacheManager.stream().toArray(CacheManager[]::new);
        CompositeCacheManager compositeCacheManager;
        if (managers.length > 0) {
            CacheManager[] ordered = new CacheManager[managers.length + 1];
            System.arraycopy(managers, 0, ordered, 0, managers.length);
            ordered[managers.length] = caffeineCacheManager;
            compositeCacheManager = new CompositeCacheManager(ordered);
        } else {
            compositeCacheManager = new CompositeCacheManager(caffeineCacheManager);
        }
        compositeCacheManager.setFallbackToNoOpCache(false);
        return compositeCacheManager;
    }
}

@ConfigurationProperties(prefix = "cache")
class CacheTtlProperties {

    private long localTtlSeconds = 300;
    private long redisTtlSeconds = 600;
    private long localMaximumSize = 10_000;

    public long getLocalTtlSeconds() {
        return localTtlSeconds;
    }

    public void setLocalTtlSeconds(long localTtlSeconds) {
        this.localTtlSeconds = localTtlSeconds;
    }

    public long getRedisTtlSeconds() {
        return redisTtlSeconds;
    }

    public void setRedisTtlSeconds(long redisTtlSeconds) {
        this.redisTtlSeconds = redisTtlSeconds;
    }

    public long getLocalMaximumSize() {
        return localMaximumSize;
    }

    public void setLocalMaximumSize(long localMaximumSize) {
        this.localMaximumSize = localMaximumSize;
    }
}
