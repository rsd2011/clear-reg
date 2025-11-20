package com.example.server.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.example.common.cache.CacheInvalidationEvent;
import com.example.common.cache.CacheInvalidationPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(prefix = "cache.invalidation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisCacheInvalidationPublisher implements CacheInvalidationPublisher {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheInvalidationPublisher.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheInvalidationProperties properties;

    public RedisCacheInvalidationPublisher(StringRedisTemplate redisTemplate,
                                           ObjectMapper objectMapper,
                                           CacheInvalidationProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public void publish(CacheInvalidationEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(properties.getChannel(), payload);
        }
        catch (JsonProcessingException e) {
            log.error("Failed to serialize cache invalidation event {}", event, e);
        }
    }
}
