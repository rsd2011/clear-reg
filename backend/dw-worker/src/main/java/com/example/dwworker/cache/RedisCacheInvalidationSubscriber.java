package com.example.dwworker.cache;

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
 * DW 워커용 캐시 무효화 수신기. 캐시 없는 워커이지만 이벤트를 수신해 로그 남기고 추후 확장한다.
 */
@Component
@ConditionalOnProperty(prefix = "cache.invalidation", name = "enabled", havingValue = "true")
public class RedisCacheInvalidationSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheInvalidationSubscriber.class);

    public RedisCacheInvalidationSubscriber(RedisMessageListenerContainer container,
                                            StringRedisTemplate redisTemplate,
                                            CacheInvalidationProperties properties) {
        container.addMessageListener(this, new ChannelTopic(properties.getChannel()));
        log.info("dw-worker subscribed cache invalidation channel {}", properties.getChannel());
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        log.info("dw-worker received cache invalidation message: {}", message);
    }
}
