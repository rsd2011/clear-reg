package com.example.batch.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * 배치 앱 캐시 무효화 수신기. 현재 캐시를 사용하지 않지만 이벤트 기록을 남기며 후속 확장 대비.
 */
@Component
@ConditionalOnProperty(prefix = "cache.invalidation", name = "enabled", havingValue = "true")
public class RedisCacheInvalidationSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheInvalidationSubscriber.class);

    public RedisCacheInvalidationSubscriber(RedisMessageListenerContainer container,
                                            StringRedisTemplate redisTemplate,
                                            CacheInvalidationProperties properties) {
        container.addMessageListener(this, new ChannelTopic(properties.getChannel()));
        log.info("batch-app subscribed cache invalidation channel {}", properties.getChannel());
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        log.info("batch-app received cache invalidation message: {}", message);
    }
}
