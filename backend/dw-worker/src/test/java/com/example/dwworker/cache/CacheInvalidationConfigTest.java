package com.example.dwworker.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

class CacheInvalidationConfigTest {

    private final CacheInvalidationConfig config = new CacheInvalidationConfig();

    @Test
    @DisplayName("RedisMessageListenerContainer에 ConnectionFactory가 설정된다")
    void createsContainerWithConnectionFactory() {
        // Given
        RedisConnectionFactory connectionFactory = new LettuceConnectionFactory();

        // When
        RedisMessageListenerContainer container = config.redisMessageListenerContainer(connectionFactory);

        // Then
        assertThat(container.getConnectionFactory()).isSameAs(connectionFactory);
    }
}
