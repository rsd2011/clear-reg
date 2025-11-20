package com.example.server.readmodel;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.example.dw.application.readmodel.MenuReadModel;
import com.example.dw.application.readmodel.MenuReadModelPort;
import com.example.dw.application.readmodel.MenuReadModelSource;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(prefix = "readmodel.menu", name = "enabled", havingValue = "true")
public class RedisMenuReadModelPort implements MenuReadModelPort {

    private static final Logger log = LoggerFactory.getLogger(RedisMenuReadModelPort.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MenuReadModelSource menuReadModelSource;
    private final MenuReadModelProperties properties;

    public RedisMenuReadModelPort(StringRedisTemplate redisTemplate,
                                  ObjectMapper objectMapper,
                                  MenuReadModelSource menuReadModelSource,
                                  MenuReadModelProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.menuReadModelSource = menuReadModelSource;
        this.properties = properties;
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Override
    public Optional<MenuReadModel> load() {
        if (!isEnabled()) {
            return Optional.empty();
        }
        String payload = redisTemplate.opsForValue().get(key());
        if (payload == null) {
            if (properties.isRefreshOnMiss()) {
                return Optional.of(rebuild());
            }
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(payload, MenuReadModel.class));
        }
        catch (Exception ex) {
            log.warn("Failed to deserialize menu read model, rebuilding...", ex);
            return Optional.of(rebuild());
        }
    }

    @Override
    public MenuReadModel rebuild() {
        MenuReadModel model = menuReadModelSource.snapshot();
        MenuReadModel enriched = new MenuReadModel(
                stableVersion(model),
                model.generatedAt() != null ? model.generatedAt() : OffsetDateTime.now(),
                model.items()
        );
        persist(enriched);
        return enriched;
    }

    @Override
    public void evict() {
        redisTemplate.delete(key());
    }

    private void persist(MenuReadModel model) {
        try {
            String payload = objectMapper.writeValueAsString(model);
            redisTemplate.opsForValue().set(key(), payload, properties.getTtl());
        }
        catch (Exception ex) {
            throw new IllegalStateException("Failed to write menu read model to Redis", ex);
        }
    }

    private String key() {
        return properties.getKeyPrefix() + ":" + properties.getTenantId();
    }

    private String stableVersion(MenuReadModel model) {
        if (model.version() != null && !model.version().isBlank()) {
            return model.version();
        }
        return UUID.randomUUID().toString();
    }
}
