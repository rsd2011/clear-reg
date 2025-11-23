package com.example.batch.cache;

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
import org.springframework.data.redis.connection.Message;
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
    Message message;

    @Test
    @DisplayName("캐시 무효화 채널에 구독을 등록한다")
    void subscribesCacheInvalidationChannel() {
        // Given: 캐시 무효화 채널 설정이 주어졌을 때
        CacheInvalidationProperties properties = new CacheInvalidationProperties();
        properties.setEnabled(true);
        properties.setChannel("cache-invalidation");

        // When: 구독자가 생성되면
        RedisCacheInvalidationSubscriber subscriber =
                new RedisCacheInvalidationSubscriber(container, redisTemplate, properties);

        // Then: 컨테이너에 리스너가 채널과 함께 등록된다
        ArgumentCaptor<ChannelTopic> topicCaptor = ArgumentCaptor.forClass(ChannelTopic.class);
        verify(container).addMessageListener(any(MessageListener.class), topicCaptor.capture());
        assertThat(topicCaptor.getValue().getTopic()).isEqualTo("cache-invalidation");
        assertThat(subscriber).isNotNull();
    }

    @Test
    @DisplayName("캐시 무효화 메시지를 수신하면 로그만 남긴다")
    void logsOnMessage() {
        CacheInvalidationProperties properties = new CacheInvalidationProperties();
        properties.setEnabled(true);
        RedisCacheInvalidationSubscriber subscriber =
                new RedisCacheInvalidationSubscriber(container, redisTemplate, properties);

        subscriber.onMessage(message, null);
        // 검증 대상은 로깅뿐이므로 예외가 발생하지 않음을 확인
        assertThat(subscriber).isNotNull();
    }
}
