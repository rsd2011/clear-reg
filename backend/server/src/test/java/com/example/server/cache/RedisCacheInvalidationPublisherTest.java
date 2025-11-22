package com.example.server.cache;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.example.common.cache.CacheInvalidationEvent;
import com.example.common.cache.CacheInvalidationType;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class RedisCacheInvalidationPublisherTest {

    @Mock
    StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("이벤트를 JSON으로 직렬화하여 Redis 채널로 발행한다")
    void publishesEventToRedisChannel() {
        CacheInvalidationProperties properties = new CacheInvalidationProperties();
        properties.setChannel("cache-invalidation");
        RedisCacheInvalidationPublisher publisher = new RedisCacheInvalidationPublisher(redisTemplate, objectMapper, properties);
        CacheInvalidationEvent event = new CacheInvalidationEvent(CacheInvalidationType.ORGANIZATION, "trace", "subj", 1L, java.time.Instant.now());

        assertThatNoException().isThrownBy(() -> publisher.publish(event));
        verify(redisTemplate).convertAndSend(anyString(), anyString());
    }
}
