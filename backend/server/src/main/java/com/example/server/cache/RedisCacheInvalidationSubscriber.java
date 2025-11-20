package com.example.server.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import com.example.common.cache.CacheInvalidationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(prefix = "cache.invalidation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisCacheInvalidationSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheInvalidationSubscriber.class);

    private final CacheInvalidationHandler handler;
    private final ObjectMapper objectMapper;

    public RedisCacheInvalidationSubscriber(RedisMessageListenerContainer container,
                                            StringRedisTemplate redisTemplate,
                                            CacheInvalidationProperties properties,
                                            CacheInvalidationHandler handler,
                                            ObjectMapper objectMapper) {
        this.handler = handler;
        this.objectMapper = objectMapper;
        String channel = properties.getChannel();
        container.addMessageListener(this, new ChannelTopic(channel));
        log.info("Subscribed to cache invalidation channel '{}'", channel);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            CacheInvalidationEvent event = objectMapper.readValue(message.getBody(), CacheInvalidationEvent.class);
            handler.handle(event);
        }
        catch (Exception ex) {
            log.error("Failed to process cache invalidation message: {}", message, ex);
        }
    }
}
