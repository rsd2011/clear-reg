package com.example.server.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import com.example.common.cache.CacheInvalidationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class RedisCacheInvalidationSubscriberTest {

    @Mock RedisMessageListenerContainer container;
    @Mock StringRedisTemplate redisTemplate;
    @Mock CacheInvalidationHandler handler;
    @Mock Message message;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("구독 시 설정된 채널에 리스너를 등록한다")
    void subscribesToChannel() {
        CacheInvalidationProperties properties = new CacheInvalidationProperties();
        properties.setChannel("cache-invalidation");

        RedisCacheInvalidationSubscriber subscriber = new RedisCacheInvalidationSubscriber(
                container, redisTemplate, properties, handler, objectMapper);

        ArgumentCaptor<ChannelTopic> topicCaptor = ArgumentCaptor.forClass(ChannelTopic.class);
        verify(container).addMessageListener(any(), topicCaptor.capture());
        assertThat(topicCaptor.getValue().getTopic()).isEqualTo("cache-invalidation");
        assertThat(subscriber).isNotNull();
    }

    @Test
    @DisplayName("유효한 메시지를 수신하면 핸들러에 위임한다")
    void delegatesValidMessage() throws Exception {
        CacheInvalidationProperties properties = new CacheInvalidationProperties();
        properties.setChannel("cache-invalidation");
        RedisCacheInvalidationSubscriber subscriber = new RedisCacheInvalidationSubscriber(
                container, redisTemplate, properties, handler, objectMapper);
        CacheInvalidationEvent event = new CacheInvalidationEvent(
                com.example.common.cache.CacheInvalidationType.ORGANIZATION,
                "trace", "subject", 1L, java.time.Instant.now());
        byte[] body = objectMapper.writeValueAsBytes(event);
        org.springframework.data.redis.connection.DefaultMessage msg =
                new org.springframework.data.redis.connection.DefaultMessage("ch".getBytes(), body);

        subscriber.onMessage(msg, null);

        verify(handler).handle(any(CacheInvalidationEvent.class));
    }
}
