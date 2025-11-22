package com.example.dwworker.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@ExtendWith(MockitoExtension.class)
class RedisCacheInvalidationSubscriberTest {

    @Mock
    RedisMessageListenerContainer container;
    @Mock
    StringRedisTemplate redisTemplate;
    @Mock
    org.springframework.data.redis.connection.Message message;

    @Test
    @DisplayName("워커는 캐시 무효화 채널을 구독하여 메시지를 수신한다")
    void subscribesCacheInvalidationChannel() {
        // Given
        CacheInvalidationProperties properties = new CacheInvalidationProperties();
        properties.setEnabled(true);
        properties.setChannel("cache-invalidation");

        // When
        RedisCacheInvalidationSubscriber subscriber =
                new RedisCacheInvalidationSubscriber(container, redisTemplate, properties);

        // Then
        ArgumentCaptor<ChannelTopic> topicCaptor = ArgumentCaptor.forClass(ChannelTopic.class);
        verify(container).addMessageListener(any(MessageListener.class), topicCaptor.capture());
        assertThat(topicCaptor.getValue().getTopic()).isEqualTo("cache-invalidation");
        assertThat(subscriber).isNotNull();
    }

    @Test
    @DisplayName("캐시 무효화 메시지를 수신하면 예외 없이 처리한다")
    void handlesIncomingMessage() {
        CacheInvalidationProperties properties = new CacheInvalidationProperties();
        properties.setEnabled(true);
        RedisCacheInvalidationSubscriber subscriber =
                new RedisCacheInvalidationSubscriber(container, redisTemplate, properties);

        subscriber.onMessage(message, null);
        assertThat(subscriber).isNotNull();
    }
}
